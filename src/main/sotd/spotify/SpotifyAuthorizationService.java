package sotd.spotify;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import sotd.crypto.TokenEncryptionService;
import sotd.spotify.client.SpotifyAccountsClient;
import sotd.spotify.client.SpotifyApiClient;
import sotd.spotify.client.SpotifyCurrentUserProfile;
import sotd.spotify.client.SpotifyTokenResponse;
import sotd.spotify.persistence.SpotifyAccountRepository;

@Service
public class SpotifyAuthorizationService {

    private final SpotifyProperties spotifyProperties;
    private final SpotifyAuthStateStore authStateStore;
    private final SpotifyAccountsClient spotifyAccountsClient;
    private final SpotifyApiClient spotifyApiClient;
    private final TokenEncryptionService tokenEncryptionService;
    private final SpotifyAccountRepository spotifyAccountRepository;
    private final Clock clock;

    public SpotifyAuthorizationService(
            SpotifyProperties spotifyProperties,
            SpotifyAuthStateStore authStateStore,
            SpotifyAccountsClient spotifyAccountsClient,
            SpotifyApiClient spotifyApiClient,
            TokenEncryptionService tokenEncryptionService,
            SpotifyAccountRepository spotifyAccountRepository,
            Clock clock
    ) {
        this.spotifyProperties = spotifyProperties;
        this.authStateStore = authStateStore;
        this.spotifyAccountsClient = spotifyAccountsClient;
        this.spotifyApiClient = spotifyApiClient;
        this.tokenEncryptionService = tokenEncryptionService;
        this.spotifyAccountRepository = spotifyAccountRepository;
        this.clock = clock;
    }

    public URI buildAuthorizationUri() {
        requireConfiguredCredentials();

        Instant now = clock.instant();
        String state = authStateStore.issueState(now.plus(spotifyProperties.getAuthStateTtl()));

        return UriComponentsBuilder.fromUri(spotifyProperties.getAccountsBaseUrl())
                .path("/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", spotifyProperties.getClientId())
                .queryParam("redirect_uri", spotifyProperties.getRedirectUri())
                .queryParam("scope", joinScopes(spotifyProperties.getScopes()))
                .queryParam("state", state)
                .queryParam("show_dialog", spotifyProperties.isShowDialog())
                .build()
                .encode()
                .toUri();
    }

    public SpotifyConnectionResponse handleCallback(String code, String state, String error) {
        requireConfiguredCredentials();

        if (StringUtils.hasText(error)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spotify authorization failed: " + error);
        }
        if (!StringUtils.hasText(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spotify callback is missing the authorization code.");
        }
        if (!authStateStore.consume(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spotify callback state is invalid or expired.");
        }

        SpotifyTokenResponse tokenResponse = spotifyAccountsClient.exchangeAuthorizationCode(
                spotifyProperties.getClientId(),
                spotifyProperties.getClientSecret(),
                code,
                spotifyProperties.getRedirectUri()
        );

        if (!StringUtils.hasText(tokenResponse.refreshToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spotify did not return a refresh token.");
        }

        SpotifyCurrentUserProfile userProfile = spotifyApiClient.getCurrentUserProfile(tokenResponse.accessToken());
        Instant tokenExpiresAt = clock.instant().plusSeconds(tokenResponse.expiresIn());

        spotifyAccountRepository.saveOrUpdate(
                userProfile,
                tokenEncryptionService.encrypt(tokenResponse.refreshToken()),
                tokenResponse.scope(),
                tokenExpiresAt,
                clock.instant(),
                ZoneId.systemDefault().getId()
        );

        return new SpotifyConnectionResponse(
                "connected",
                userProfile.id(),
                userProfile.displayName(),
                tokenResponse.scope(),
                tokenExpiresAt
        );
    }

    public Optional<SpotifyLinkedAccountView> getCurrentConnection() {
        return spotifyAccountRepository.findMostRecent();
    }

    private void requireConfiguredCredentials() {
        if (!StringUtils.hasText(spotifyProperties.getClientId())
                || !StringUtils.hasText(spotifyProperties.getClientSecret())) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Spotify client credentials are not configured. Add SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET to .env."
            );
        }
    }

    private static String joinScopes(List<String> scopes) {
        StringJoiner joiner = new StringJoiner(" ");
        scopes.stream()
                .filter(StringUtils::hasText)
                .forEach(joiner::add);
        return joiner.toString();
    }
}
