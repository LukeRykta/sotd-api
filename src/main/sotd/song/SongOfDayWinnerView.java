package sotd.song;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-model projection for a computed song-of-the-day winner.
 */
public record SongOfDayWinnerView(
        UUID appUserId,
        String spotifyUserId,
        String displayName,
        String timezone,
        LocalDate periodStartLocal,
        String spotifyTrackId,
        String trackName,
        int playCount
) {
}
