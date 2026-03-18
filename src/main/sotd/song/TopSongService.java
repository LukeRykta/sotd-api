package sotd.song;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import sotd.spotify.persistence.SpotifyAccountRepository;
import sotd.spotify.persistence.SpotifyAccountRepository.LinkedSpotifyAccountIdentity;

/**
 * Resolves the highest-ranked song for one user in the requested period window.
 */
@Service
public class TopSongService {

    private final TopSongRepository topSongRepository;
    private final SpotifyAccountRepository spotifyAccountRepository;
    private final Clock clock;

    public TopSongService(
            TopSongRepository topSongRepository,
            SpotifyAccountRepository spotifyAccountRepository,
            Clock clock
    ) {
        this.topSongRepository = topSongRepository;
        this.spotifyAccountRepository = spotifyAccountRepository;
        this.clock = clock;
    }

    public TopSongResponse getTopSong(UUID appUserId, SongPeriodType periodType) {
        Optional<LinkedSpotifyAccountIdentity> linkedAccount = spotifyAccountRepository.findAccountIdentityByAppUserId(appUserId);
        if (linkedAccount.isEmpty()) {
            return TopSongResponse.unlinked(appUserId, periodType, null);
        }

        LocalDate anchorDate = clock.instant()
                .atZone(ZoneId.of(linkedAccount.get().timezone()))
                .toLocalDate();
        SongPeriodType.PeriodWindow window = periodType.resolveWindow(anchorDate);

        return topSongRepository.findTopSong(
                        appUserId,
                        periodType,
                        window.periodStartLocal(),
                        window.periodEndExclusive()
                )
                .map(TopSongResponse::available)
                .orElseGet(() -> TopSongResponse.pending(
                        appUserId,
                        periodType,
                        window.periodStartLocal()
                ));
    }
}
