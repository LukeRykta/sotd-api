package sotd.song;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import sotd.spotify.persistence.SpotifyAccountRepository;
import sotd.spotify.persistence.SpotifyAccountRepository.LinkedSpotifyAccountIdentity;

@Service
public class SongOfDayService {

    private final SongOfDayRepository songOfDayRepository;
    private final SpotifyAccountRepository spotifyAccountRepository;
    private final Clock clock;

    public SongOfDayService(
            SongOfDayRepository songOfDayRepository,
            SpotifyAccountRepository spotifyAccountRepository,
            Clock clock
    ) {
        this.songOfDayRepository = songOfDayRepository;
        this.spotifyAccountRepository = spotifyAccountRepository;
        this.clock = clock;
    }

    public SongOfDayResponse getCurrentSongOfDay(UUID appUserId) {
        Optional<LinkedSpotifyAccountIdentity> linkedAccount = spotifyAccountRepository.findAccountIdentityByAppUserId(appUserId);
        if (linkedAccount.isEmpty()) {
            return SongOfDayResponse.unlinked(appUserId);
        }

        LocalDate localDate = clock.instant()
                .atZone(ZoneId.of(linkedAccount.get().timezone()))
                .toLocalDate();

        return songOfDayRepository.findCurrentWinner(appUserId, localDate)
                .map(SongOfDayResponse::available)
                .orElseGet(() -> SongOfDayResponse.pending(appUserId));
    }
}
