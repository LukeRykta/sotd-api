package sotd.spotify;

import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts callback-specific failures into frontend redirects with stable query parameters.
 */
@RestControllerAdvice(assignableTypes = SpotifyAuthorizationController.class)
public class SpotifyAuthorizationControllerAdvice {

    private final SpotifyCallbackRedirectService spotifyCallbackRedirectService;

    public SpotifyAuthorizationControllerAdvice(SpotifyCallbackRedirectService spotifyCallbackRedirectService) {
        this.spotifyCallbackRedirectService = spotifyCallbackRedirectService;
    }

    @ExceptionHandler(SpotifyCallbackException.class)
    ResponseEntity<Void> handleSpotifyCallbackException(SpotifyCallbackException ex) {
        URI frontendRedirect = spotifyCallbackRedirectService.buildFailureRedirect(ex);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, frontendRedirect.toString())
                .build();
    }
}
