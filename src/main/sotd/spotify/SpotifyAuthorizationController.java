package sotd.spotify;

import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyAuthorizationController {

    private final SpotifyAuthorizationService spotifyAuthorizationService;

    public SpotifyAuthorizationController(SpotifyAuthorizationService spotifyAuthorizationService) {
        this.spotifyAuthorizationService = spotifyAuthorizationService;
    }

    @GetMapping("/connect")
    public ResponseEntity<Void> connect() {
        URI authorizeUri = spotifyAuthorizationService.buildAuthorizationUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizeUri.toString())
                .build();
    }

    @GetMapping("/callback")
    public SpotifyConnectionResponse callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        return spotifyAuthorizationService.handleCallback(code, state, error);
    }

    @GetMapping("/connection")
    public ResponseEntity<SpotifyLinkedAccountView> getConnection() {
        return spotifyAuthorizationService.getCurrentConnection()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
