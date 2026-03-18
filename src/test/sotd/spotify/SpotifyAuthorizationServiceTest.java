package sotd.spotify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import sotd.crypto.CryptoProperties;
import sotd.crypto.TokenEncryptionService;
import sotd.spotify.client.SpotifyAccountsClient;
import sotd.spotify.client.SpotifyApiClient;
import sotd.spotify.client.SpotifyCurrentUserProfile;
import sotd.spotify.client.SpotifyTokenResponse;
import sotd.spotify.persistence.SpotifyAccountRepository;

class SpotifyAuthorizationServiceTest {

    @Test
    void buildAuthorizationUriIncludesExpectedSpotifyParameters() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-17T20:00:00Z"), ZoneOffset.UTC);
        SpotifyProperties properties = configuredProperties();
        SpotifyAuthStateStore stateStore = mock(SpotifyAuthStateStore.class);
        when(stateStore.issueState(Instant.parse("2026-03-17T20:10:00Z"))).thenReturn("state-123");

        SpotifyAuthorizationService service = new SpotifyAuthorizationService(
                properties,
                stateStore,
                mock(SpotifyAccountsClient.class),
                mock(SpotifyApiClient.class),
                encryptionService(),
                mock(SpotifyAccountRepository.class),
                clock
        );

        URI authorizationUri = service.buildAuthorizationUri();

        assertThat(authorizationUri.toString())
                .isEqualTo("https://accounts.spotify.test/authorize?response_type=code&client_id=client-id&redirect_uri=http://127.0.0.1:8080/api/spotify/callback&scope=user-read-private%20user-read-recently-played&state=state-123&show_dialog=true");
    }

    @Test
    void handleCallbackExchangesCodePersistsAccountAndReturnsConnectionSummary() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-17T20:00:00Z"), ZoneOffset.UTC);
        SpotifyProperties properties = configuredProperties();
        SpotifyAuthStateStore stateStore = mock(SpotifyAuthStateStore.class);
        when(stateStore.consume("state-123")).thenReturn(true);

        SpotifyAccountsClient accountsClient = mock(SpotifyAccountsClient.class);
        when(accountsClient.exchangeAuthorizationCode(anyString(), anyString(), anyString(), any(URI.class)))
                .thenReturn(new SpotifyTokenResponse(
                        "access-token",
                        "Bearer",
                        "user-read-private user-read-recently-played",
                        3600,
                        "refresh-token"
                ));

        SpotifyApiClient apiClient = mock(SpotifyApiClient.class);
        when(apiClient.getCurrentUserProfile("access-token"))
                .thenReturn(new SpotifyCurrentUserProfile("spotify-user", "Luke"));

        SpotifyAccountRepository repository = mock(SpotifyAccountRepository.class);

        SpotifyAuthorizationService service = new SpotifyAuthorizationService(
                properties,
                stateStore,
                accountsClient,
                apiClient,
                encryptionService(),
                repository,
                clock
        );

        SpotifyConnectionResponse response = service.handleCallback("code-123", "state-123", null);

        assertThat(response.status()).isEqualTo("connected");
        assertThat(response.spotifyUserId()).isEqualTo("spotify-user");
        assertThat(response.displayName()).isEqualTo("Luke");
        assertThat(response.grantedScope()).isEqualTo("user-read-private user-read-recently-played");
        assertThat(response.accessTokenExpiresAt()).isEqualTo(Instant.parse("2026-03-17T21:00:00Z"));

        verify(repository).saveOrUpdate(
                any(SpotifyCurrentUserProfile.class),
                any(byte[].class),
                anyString(),
                any(Instant.class),
                any(Instant.class),
                anyString()
        );
    }

    @Test
    void handleCallbackRejectsInvalidState() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-17T20:00:00Z"), ZoneOffset.UTC);
        SpotifyProperties properties = configuredProperties();
        SpotifyAuthStateStore stateStore = mock(SpotifyAuthStateStore.class);
        when(stateStore.consume("bad-state")).thenReturn(false);

        SpotifyAuthorizationService service = new SpotifyAuthorizationService(
                properties,
                stateStore,
                mock(SpotifyAccountsClient.class),
                mock(SpotifyApiClient.class),
                encryptionService(),
                mock(SpotifyAccountRepository.class),
                clock
        );

        assertThatThrownBy(() -> service.handleCallback("code-123", "bad-state", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("state is invalid or expired");
    }

    private static SpotifyProperties configuredProperties() {
        SpotifyProperties properties = new SpotifyProperties();
        properties.setAccountsBaseUrl(URI.create("https://accounts.spotify.test"));
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRedirectUri(URI.create("http://127.0.0.1:8080/api/spotify/callback"));
        properties.setScopes(List.of("user-read-private", "user-read-recently-played"));
        properties.setShowDialog(true);
        properties.setAuthStateTtl(Duration.ofMinutes(10));
        return properties;
    }

    private static TokenEncryptionService encryptionService() {
        CryptoProperties cryptoProperties = new CryptoProperties();
        cryptoProperties.setBase64Key("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        return new TokenEncryptionService(cryptoProperties);
    }
}
