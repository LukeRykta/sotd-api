package sotd.song;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Shared-song result for two application users.")
public record OurSongResponse(
        @Schema(description = "High-level response state.", allowableValues = {"ready", "no-common-song", "unlinked"}, example = "ready")
        String status,
        @Schema(description = "Human-readable explanation of the current state.", example = "Shared-song data is available.")
        String message,
        @Schema(description = "Stable upstream application user UUID for the requesting profile.", example = "11111111-1111-1111-1111-111111111111")
        UUID appUserId,
        @Schema(description = "Stable upstream application user UUID for the comparison profile.", example = "22222222-2222-2222-2222-222222222222")
        UUID otherUserId,
        @Schema(description = "Requested comparison period.", example = "DAY")
        SongPeriodType periodType,
        @Schema(description = "Local period start used for the comparison window.", example = "2026-03-18")
        LocalDate periodStartLocal,
        @Schema(description = "Winning Spotify track id when a shared song exists.", example = "5u6y4u5EgDv0peILf60H5t", nullable = true)
        String spotifyTrackId,
        @Schema(description = "Winning shared track name when available.", example = "Oye Como Va", nullable = true)
        String trackName,
        @Schema(description = "Album artwork URL for the shared track when available.", example = "https://i.scdn.co/image/ab67616d0000b273...", nullable = true)
        String imageUrl,
        @Schema(description = "Play count for the requesting user within the selected window.", example = "3", nullable = true)
        Integer userPlayCount,
        @Schema(description = "Play count for the comparison user within the selected window.", example = "2", nullable = true)
        Integer otherUserPlayCount,
        @Schema(description = "Combined play count used as the primary ranking signal.", example = "5", nullable = true)
        Integer combinedPlayCount,
        @Schema(description = "Deterministic tie-break rule applied when multiple common songs tie.", example = "LEAST_SHARED_COUNT_THEN_COMBINED_PLAY_COUNT_THEN_DURATION_THEN_LAST_PLAYED_THEN_TRACK_ID", nullable = true)
        String tieBreakRule
) {

    public static OurSongResponse unlinked(
            UUID appUserId,
            UUID otherUserId,
            SongPeriodType periodType,
            LocalDate periodStartLocal,
            String message
    ) {
        return new OurSongResponse(
                "unlinked",
                message,
                appUserId,
                otherUserId,
                periodType,
                periodStartLocal,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static OurSongResponse noCommonSong(
            UUID appUserId,
            UUID otherUserId,
            SongPeriodType periodType,
            LocalDate periodStartLocal
    ) {
        return new OurSongResponse(
                "no-common-song",
                "No common song was found for the requested period.",
                appUserId,
                otherUserId,
                periodType,
                periodStartLocal,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static OurSongResponse available(OurSongMatchView match) {
        return new OurSongResponse(
                "ready",
                "Shared-song data is available.",
                match.appUserId(),
                match.otherUserId(),
                match.periodType(),
                match.periodStartLocal(),
                match.spotifyTrackId(),
                match.trackName(),
                match.imageUrl(),
                match.userPlayCount(),
                match.otherUserPlayCount(),
                match.combinedPlayCount(),
                match.tieBreakRule()
        );
    }
}
