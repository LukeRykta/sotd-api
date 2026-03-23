package sotd.song;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-model projection for the highest-ranked song in one period for one user.
 */
public record TopSongWinnerView(
        UUID appUserId,
        String spotifyUserId,
        String displayName,
        String timezone,
        SongPeriodType periodType,
        LocalDate periodStartLocal,
        String spotifyTrackId,
        String trackName,
        String artistName,
        String imageUrl,
        int playCount,
        String tieBreakRule
) {
}
