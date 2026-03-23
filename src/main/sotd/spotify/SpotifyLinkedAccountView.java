package sotd.spotify;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Linked Spotify account summary for a specific application user.")
public record SpotifyLinkedAccountView(
        @Schema(description = "Stable upstream application user UUID.", example = "11111111-1111-1111-1111-111111111111")
        UUID appUserId,
        @Schema(description = "Linked Spotify user id.", example = "lukerykta")
        String spotifyUserId,
        @Schema(description = "Spotify display name.", example = "lukerykta")
        String displayName,
        @Schema(description = "Currently stored Spotify scopes.", example = "user-read-playback-state user-read-currently-playing user-read-recently-played user-read-private")
        String scope,
        @Schema(description = "Internal account linkage state.", allowableValues = {"ACTIVE", "REAUTH_REQUIRED", "DISCONNECTED"}, example = "ACTIVE")
        String status,
        @Schema(description = "IANA timezone used for local-day rollups.", example = "America/Chicago")
        String timezone,
        @Schema(description = "When the current access token will expire.", example = "2026-03-18T18:44:06.289105Z")
        Instant accessTokenExpiresAt,
        @Schema(description = "When the linked account row was created.", example = "2026-03-17T23:50:04.677690Z")
        Instant createdAt,
        @Schema(description = "When the linked account row was last updated.", example = "2026-03-18T17:44:06.554355Z")
        Instant updatedAt
) {
}
