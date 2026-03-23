package sotd.spotify;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpotifyCallbackRedirectServiceTest {

    @Test
    void buildSuccessRedirectAppendsConnectionStatusParams() {
        SpotifyProperties properties = new SpotifyProperties();
        properties.setCallbackFrontendRedirectUri(URI.create("https://app.example.com/home?from=spotify"));
        SpotifyCallbackRedirectService redirectService = new SpotifyCallbackRedirectService(properties);
        SpotifyConnectionResponse response = new SpotifyConnectionResponse(
                "connected",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "spotify-user",
                "Luke",
                "user-read-private",
                Instant.parse("2026-03-17T21:00:00Z")
        );

        URI redirect = redirectService.buildSuccessRedirect(response);

        assertThat(redirect.toString())
                .isEqualTo(
                        "https://app.example.com/home?from=spotify&spotifyAuthStatus=connected"
                                + "&appUserId=11111111-1111-1111-1111-111111111111"
                );
    }

    @Test
    void buildFailureRedirectAppendsErrorParamsAndOmitsMissingAppUserId() {
        SpotifyProperties properties = new SpotifyProperties();
        properties.setCallbackFrontendRedirectUri(URI.create("https://app.example.com/"));
        SpotifyCallbackRedirectService redirectService = new SpotifyCallbackRedirectService(properties);

        URI redirect = redirectService.buildFailureRedirect(
                SpotifyCallbackException.invalidState("Spotify callback state is invalid or expired.")
        );

        assertThat(redirect.toString())
                .isEqualTo(
                        "https://app.example.com/?spotifyAuthStatus=error"
                                + "&spotifyAuthErrorCode=spotify_callback_invalid_state"
                                + "&spotifyAuthStage=STATE_VALIDATION"
                );
    }
}
