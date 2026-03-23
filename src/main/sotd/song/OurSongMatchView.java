package sotd.song;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-model projection for the best common song between two users for one period.
 */
public record OurSongMatchView(
        UUID appUserId,
        UUID otherUserId,
        SongPeriodType periodType,
        LocalDate periodStartLocal,
        String spotifyTrackId,
        String trackName,
        String artistName,
        String imageUrl,
        int userPlayCount,
        int otherUserPlayCount,
        int combinedPlayCount,
        String tieBreakRule
) {
}
