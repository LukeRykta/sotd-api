package sotd.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import sotd.auth.UpstreamAuthProperties;
import sotd.spotify.SpotifyProperties;

class ActuatorInfoConfigTest {

    @Test
    void infoContributorExposesUsefulOperationalMetadata() {
        Environment environment = new MockEnvironment()
                .withProperty("spring.application.name", "sotd-api")
                .withProperty("info.app.description", "Private Spotify insights API.")
                .withProperty("info.app.version", "0.0.1-SNAPSHOT");

        SpotifyProperties spotifyProperties = new SpotifyProperties();
        spotifyProperties.getPolling().setRecentlyPlayedInterval(Duration.ofMinutes(2));
        spotifyProperties.getPolling().setCurrentPlaybackInterval(Duration.ofSeconds(20));

        UpstreamAuthProperties upstreamAuthProperties = new UpstreamAuthProperties();
        upstreamAuthProperties.setEnabled(true);
        upstreamAuthProperties.setHeaderName("Authorization");
        upstreamAuthProperties.setQueryParameterName("upstreamAuth");

        Info.Builder builder = new Info.Builder();
        new ActuatorInfoConfig()
                .sotdInfoContributor(environment, spotifyProperties, upstreamAuthProperties)
                .contribute(builder);

        Info info = builder.build();

        assertThat(info.getDetails()).containsKeys("app", "docs", "features", "auth", "polling");
        assertThat((String) ((java.util.Map<?, ?>) info.getDetails().get("app")).get("name")).isEqualTo("sotd-api");
        assertThat((String) ((java.util.Map<?, ?>) info.getDetails().get("docs")).get("swaggerUiPath")).isEqualTo("/docs");
        assertThat((Boolean) ((java.util.Map<?, ?>) info.getDetails().get("features")).get("spotifyUnlink")).isTrue();
        assertThat((Boolean) ((java.util.Map<?, ?>) info.getDetails().get("auth")).get("upstreamJwtEnabled")).isTrue();
        assertThat((String) ((java.util.Map<?, ?>) info.getDetails().get("polling")).get("recentlyPlayedInterval"))
                .isEqualTo("PT2M");
    }
}
