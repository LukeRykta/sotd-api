package sotd.spotify;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sotd.config.OpenApiConfig;

@RestController
@Tag(name = "spotify-auth")
public class SpotifyAuthorizationController {

    private final SpotifyAuthorizationService spotifyAuthorizationService;
    private final SpotifyCallbackRedirectService spotifyCallbackRedirectService;

    public SpotifyAuthorizationController(
            SpotifyAuthorizationService spotifyAuthorizationService,
            SpotifyCallbackRedirectService spotifyCallbackRedirectService
    ) {
        this.spotifyAuthorizationService = spotifyAuthorizationService;
        this.spotifyCallbackRedirectService = spotifyCallbackRedirectService;
    }

    @GetMapping("/api/users/{appUserId}/spotify/connect")
    @Operation(
            summary = "Start Spotify connect flow",
            description = "Validates the upstream user token and redirects the browser to Spotify authorization.",
            security = @SecurityRequirement(name = OpenApiConfig.UPSTREAM_QUERY_AUTH_SCHEME),
            parameters = {
                    @Parameter(name = "appUserId", description = "Stable upstream application user UUID.", required = true),
                    @Parameter(
                            name = "upstreamAuth",
                            in = ParameterIn.QUERY,
                            description = "Short-lived upstream-issued JWT whose subject must match the path UUID.",
                            required = true
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirects to Spotify authorize page.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid upstream auth token.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Upstream token does not match the requested UUID.", content = @Content)
    })
    public ResponseEntity<Void> connect(@PathVariable UUID appUserId) {
        URI authorizeUri = spotifyAuthorizationService.buildAuthorizationUri(appUserId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizeUri.toString())
                .build();
    }

    @GetMapping("/api/spotify/callback")
    @Operation(
            summary = "Spotify OAuth callback",
            description = "Spotify redirects here after consent. This route exchanges the code, stores the encrypted refresh token, and then redirects the browser to the configured frontend route with callback status query parameters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirects the browser to the configured frontend route with callback status query parameters.")
    })
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        SpotifyConnectionResponse response = spotifyAuthorizationService.handleCallback(code, state, error);
        URI frontendRedirect = spotifyCallbackRedirectService.buildSuccessRedirect(response);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, frontendRedirect.toString())
                .build();
    }

    @GetMapping("/api/users/{appUserId}/spotify/connection")
    @Operation(
            summary = "Get linked Spotify account",
            description = "Returns the Spotify account currently linked to the requested application user.",
            security = @SecurityRequirement(name = OpenApiConfig.UPSTREAM_HEADER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Linked Spotify account returned.",
                    content = @Content(schema = @Schema(implementation = SpotifyLinkedAccountView.class))
            ),
            @ApiResponse(responseCode = "404", description = "No Spotify account is linked for that user.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid upstream auth token.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Upstream token does not match the requested UUID.", content = @Content)
    })
    public ResponseEntity<SpotifyLinkedAccountView> getConnection(
            @Parameter(description = "Stable upstream application user UUID.", required = true)
            @PathVariable UUID appUserId
    ) {
        return spotifyAuthorizationService.getCurrentConnection(appUserId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/users/{appUserId}/spotify/connection")
    @Operation(
            summary = "Unlink Spotify account",
            description = "Soft-disconnects the Spotify account currently linked to the requested application user.",
            security = @SecurityRequirement(name = OpenApiConfig.UPSTREAM_HEADER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Spotify account disconnected or already absent.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid upstream auth token.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Upstream token does not match the requested UUID.", content = @Content)
    })
    public ResponseEntity<Void> disconnect(
            @Parameter(description = "Stable upstream application user UUID.", required = true)
            @PathVariable UUID appUserId
    ) {
        spotifyAuthorizationService.disconnectCurrentConnection(appUserId);
        return ResponseEntity.noContent().build();
    }
}
