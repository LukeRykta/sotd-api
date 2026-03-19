package sotd.spotify;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Stable JSON error shape returned from the Spotify OAuth callback route.
 */
public record SpotifyCallbackErrorResponse(
        @Schema(description = "Fixed error status for callback failures.", example = "error")
        String status,
        @Schema(description = "Stable machine-readable callback failure code.", example = "spotify_callback_invalid_state")
        String errorCode,
        @Schema(description = "Callback processing stage that failed.", example = "STATE_VALIDATION")
        String stage,
        @Schema(description = "Human-readable callback failure message.", example = "Spotify callback state was missing, expired, or invalid.")
        String message,
        @Schema(description = "Stable upstream application user UUID when known.", example = "11111111-1111-1111-1111-111111111111", nullable = true)
        UUID appUserId,
        @Schema(description = "When the failure response was generated.", example = "2026-03-19T00:28:58.431Z")
        Instant timestamp
) {
    public static SpotifyCallbackErrorResponse from(SpotifyCallbackException ex, Instant timestamp) {
        return new SpotifyCallbackErrorResponse(
                "error",
                ex.getErrorCode(),
                ex.getStage().name(),
                ex.getUserMessage(),
                ex.getAppUserId(),
                timestamp
        );
    }
}
