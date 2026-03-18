package sotd.song;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Top-song result for a specific application user and period.")
public record TopSongResponse(
        @Schema(description = "High-level response state.", allowableValues = {"ready", "pending", "unlinked"})
        String status,
        @Schema(description = "Human-readable explanation of the current state.")
        String message,
        @Schema(description = "Stable upstream application user UUID.")
        UUID appUserId,
        @Schema(description = "Linked Spotify user id when available.")
        String spotifyUserId,
        @Schema(description = "Spotify display name when available.")
        String displayName,
        @Schema(description = "Requested ranking period.")
        SongPeriodType periodType,
        @Schema(description = "Local period start used for the winner calculation.")
        LocalDate periodStartLocal,
        @Schema(description = "Winning Spotify track id when a winner exists.")
        String spotifyTrackId,
        @Schema(description = "Winning track name when a winner exists.")
        String trackName,
        @Schema(description = "Winning play count for the selected period.")
        Integer playCount,
        @Schema(description = "Deterministic tie-break rule applied when multiple songs tie.")
        String tieBreakRule
) {

    public static TopSongResponse unlinked(UUID appUserId, SongPeriodType periodType, LocalDate periodStartLocal) {
        return new TopSongResponse(
                "unlinked",
                "No Spotify account is linked for this user.",
                appUserId,
                null,
                null,
                periodType,
                periodStartLocal,
                null,
                null,
                null,
                null
        );
    }

    public static TopSongResponse pending(UUID appUserId, SongPeriodType periodType, LocalDate periodStartLocal) {
        return new TopSongResponse(
                "pending",
                "No top-song data has been computed yet for the requested period.",
                appUserId,
                null,
                null,
                periodType,
                periodStartLocal,
                null,
                null,
                null,
                null
        );
    }

    public static TopSongResponse available(TopSongWinnerView winner) {
        return new TopSongResponse(
                "ready",
                "Top-song data is available.",
                winner.appUserId(),
                winner.spotifyUserId(),
                winner.displayName(),
                winner.periodType(),
                winner.periodStartLocal(),
                winner.spotifyTrackId(),
                winner.trackName(),
                winner.playCount(),
                winner.tieBreakRule()
        );
    }
}
