package sotd.song;

import java.time.LocalDate;
import java.util.UUID;

public record SongOfDayResponse(
        String status,
        String message,
        UUID appUserId,
        String spotifyUserId,
        String displayName,
        LocalDate periodStartLocal,
        String spotifyTrackId,
        String trackName,
        Integer playCount
) {

    public static SongOfDayResponse unlinked(UUID appUserId) {
        return new SongOfDayResponse(
                "unlinked",
                "No Spotify account is linked for this user.",
                appUserId,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static SongOfDayResponse pending(UUID appUserId) {
        return new SongOfDayResponse(
                "pending",
                "No song-of-the-day data has been computed yet.",
                appUserId,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static SongOfDayResponse available(SongOfDayWinnerView winner) {
        return new SongOfDayResponse(
                "ready",
                "Song-of-the-day data is available.",
                winner.appUserId(),
                winner.spotifyUserId(),
                winner.displayName(),
                winner.periodStartLocal(),
                winner.spotifyTrackId(),
                winner.trackName(),
                winner.playCount()
        );
    }
}
