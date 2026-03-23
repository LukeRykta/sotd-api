package sotd.song;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Top-song result for a specific application user and period.")
public record TopSongResponse(
        @Schema(description = "High-level response state.", allowableValues = {"ready", "pending", "unlinked"}, example = "ready")
        String status,
        @Schema(description = "Human-readable explanation of the current state.", example = "Top-song data is available.")
        String message,
        @Schema(description = "Stable upstream application user UUID.", example = "11111111-1111-1111-1111-111111111111")
        UUID appUserId,
        @Schema(description = "Linked Spotify user id when available.", example = "lukerykta")
        String spotifyUserId,
        @Schema(description = "Spotify display name when available.", example = "lukerykta")
        String displayName,
        @Schema(description = "Requested ranking period.", example = "DAY")
        SongPeriodType periodType,
        @Schema(description = "Local period start used for the winner calculation.", example = "2026-03-18")
        LocalDate periodStartLocal,
        @Schema(description = "Winning Spotify track id when a winner exists.", example = "5u6y4u5EgDv0peILf60H5t", nullable = true)
        String spotifyTrackId,
        @Schema(description = "Winning track name when a winner exists.", example = "Oye Como Va", nullable = true)
        String trackName,
        @Schema(description = "Winning play count for the selected period.", example = "2", nullable = true)
        Integer playCount,
        @Schema(description = "Deterministic tie-break rule applied when multiple songs tie.", example = "PLAY_COUNT_THEN_LAST_PLAYED_THEN_TRACK_ID", nullable = true)
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
