package sotd.spotify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SpotifyAuthorizationControllerAdviceTest {

    @Test
    void handleSpotifyCallbackExceptionRedirectsToConfiguredFrontendRoute() {
        SpotifyProperties properties = new SpotifyProperties();
        properties.setCallbackFrontendRedirectUri(java.net.URI.create("https://app.example.com/"));
        SpotifyAuthorizationControllerAdvice advice = new SpotifyAuthorizationControllerAdvice(
                new SpotifyCallbackRedirectService(properties)
        );
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        SpotifyCallbackException exception = SpotifyCallbackException.authorizationDenied(
                appUserId,
                "Spotify authorization was denied or cancelled."
        );

        ResponseEntity<Void> response = advice.handleSpotifyCallbackException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .isEqualTo(
                        "https://app.example.com/?spotifyAuthStatus=error&spotifyAuthErrorCode=spotify_callback_authorization_denied"
                                + "&spotifyAuthStage=AUTHORIZATION_DENIED&appUserId=11111111-1111-1111-1111-111111111111"
                );
    }
}
