package sotd.spotify;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Result returned after Spotify account linking succeeds.")
public record SpotifyConnectionResponse(
        @Schema(description = "Connection status.", allowableValues = {"connected"}, example = "connected")
        String status,
        @Schema(description = "Stable upstream application user UUID.", example = "11111111-1111-1111-1111-111111111111")
        UUID appUserId,
        @Schema(description = "Linked Spotify user id.", example = "lukerykta")
        String spotifyUserId,
        @Schema(description = "Spotify display name.", example = "lukerykta")
        String displayName,
        @Schema(description = "Granted Spotify scopes returned by the token exchange.", example = "user-read-playback-state user-read-currently-playing user-read-recently-played user-read-private")
        String grantedScope,
        @Schema(description = "When the current access token will expire.", example = "2026-03-19T00:26:41.151Z")
        Instant accessTokenExpiresAt
) {
}
