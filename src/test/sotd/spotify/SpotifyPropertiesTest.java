package sotd.spotify;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SpotifyPropertiesTest {

    @Test
    void defaultsMatchExpectedLocalConfiguration() {
        SpotifyProperties properties = new SpotifyProperties();

        assertThat(properties.getBaseUrl()).isEqualTo(URI.create("https://api.spotify.com/v1"));
        assertThat(properties.getAccountsBaseUrl()).isEqualTo(URI.create("https://accounts.spotify.com"));
        assertThat(properties.getClientId()).isEmpty();
        assertThat(properties.getClientSecret()).isEmpty();
        assertThat(properties.getRedirectUri()).isEqualTo(URI.create("http://127.0.0.1:8080/api/spotify/callback"));
        assertThat(properties.getScopes()).isEmpty();
        assertThat(properties.isShowDialog()).isFalse();
        assertThat(properties.getAuthStateTtl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.getPolling().getRecentlyPlayedInterval()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getPolling().getCurrentPlaybackInterval()).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void settersUpdateAllConfigurableValues() {
        SpotifyProperties properties = new SpotifyProperties();

        properties.setBaseUrl(URI.create("https://api.spotify.test/v1"));
        properties.setAccountsBaseUrl(URI.create("https://accounts.spotify.test"));
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRedirectUri(URI.create("http://127.0.0.1:8080/api/spotify/callback"));
        properties.setScopes(java.util.List.of("user-read-private"));
        properties.setShowDialog(true);
        properties.setAuthStateTtl(Duration.ofMinutes(15));
        properties.getPolling().setRecentlyPlayedInterval(Duration.ofMinutes(5));
        properties.getPolling().setCurrentPlaybackInterval(Duration.ofSeconds(45));

        assertThat(properties.getBaseUrl()).isEqualTo(URI.create("https://api.spotify.test/v1"));
        assertThat(properties.getAccountsBaseUrl()).isEqualTo(URI.create("https://accounts.spotify.test"));
        assertThat(properties.getClientId()).isEqualTo("client-id");
        assertThat(properties.getClientSecret()).isEqualTo("client-secret");
        assertThat(properties.getRedirectUri()).isEqualTo(URI.create("http://127.0.0.1:8080/api/spotify/callback"));
        assertThat(properties.getScopes()).containsExactly("user-read-private");
        assertThat(properties.isShowDialog()).isTrue();
        assertThat(properties.getAuthStateTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getPolling().getRecentlyPlayedInterval()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getPolling().getCurrentPlaybackInterval()).isEqualTo(Duration.ofSeconds(45));
    }
}
