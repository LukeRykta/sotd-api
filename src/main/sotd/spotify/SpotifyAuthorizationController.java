package sotd.spotify;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpotifyAuthorizationController {

    private final SpotifyAuthorizationService spotifyAuthorizationService;

    public SpotifyAuthorizationController(SpotifyAuthorizationService spotifyAuthorizationService) {
        this.spotifyAuthorizationService = spotifyAuthorizationService;
    }

    @GetMapping("/api/users/{appUserId}/spotify/connect")
    public ResponseEntity<Void> connect(@PathVariable UUID appUserId) {
        URI authorizeUri = spotifyAuthorizationService.buildAuthorizationUri(appUserId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizeUri.toString())
                .build();
    }

    @GetMapping("/api/spotify/callback")
    public SpotifyConnectionResponse callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        return spotifyAuthorizationService.handleCallback(code, state, error);
    }

    @GetMapping("/api/users/{appUserId}/spotify/connection")
    public ResponseEntity<SpotifyLinkedAccountView> getConnection(@PathVariable UUID appUserId) {
        return spotifyAuthorizationService.getCurrentConnection(appUserId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
