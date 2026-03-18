package sotd.song;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sotd.spotify.persistence.SpotifyAccountRepository;
import sotd.spotify.persistence.SpotifyAccountRepository.LinkedSpotifyAccountIdentity;

class SongOfDayServiceTest {

    @Test
    void getCurrentSongOfDayReturnsCurrentWinnerForRequestedAppUser() {
        SongOfDayRepository songOfDayRepository = mock(SongOfDayRepository.class);
        SpotifyAccountRepository spotifyAccountRepository = mock(SpotifyAccountRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-18T03:00:00Z"), ZoneOffset.UTC);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(spotifyAccountRepository.findAccountIdentityByAppUserId(appUserId))
                .thenReturn(Optional.of(new LinkedSpotifyAccountIdentity(appUserId, "lukerykta", "America/New_York")));
        when(songOfDayRepository.findCurrentWinner(appUserId, LocalDate.parse("2026-03-17")))
                .thenReturn(Optional.of(new SongOfDayWinnerView(
                        appUserId,
                        "lukerykta",
                        "Luke",
                        "America/New_York",
                        LocalDate.parse("2026-03-17"),
                        "track-1",
                        "Track Name",
                        4
                )));

        SongOfDayService service = new SongOfDayService(songOfDayRepository, spotifyAccountRepository, clock);

        SongOfDayResponse response = service.getCurrentSongOfDay(appUserId);

        assertThat(response.status()).isEqualTo("ready");
        assertThat(response.appUserId()).isEqualTo(appUserId);
        assertThat(response.spotifyUserId()).isEqualTo("lukerykta");
        assertThat(response.trackName()).isEqualTo("Track Name");
        assertThat(response.playCount()).isEqualTo(4);
    }

    @Test
    void getCurrentSongOfDayReturnsUnlinkedWhenNoAccountIsLinked() {
        SongOfDayRepository songOfDayRepository = mock(SongOfDayRepository.class);
        SpotifyAccountRepository spotifyAccountRepository = mock(SpotifyAccountRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-18T03:00:00Z"), ZoneOffset.UTC);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(spotifyAccountRepository.findAccountIdentityByAppUserId(appUserId)).thenReturn(Optional.empty());

        SongOfDayService service = new SongOfDayService(songOfDayRepository, spotifyAccountRepository, clock);

        SongOfDayResponse response = service.getCurrentSongOfDay(appUserId);

        assertThat(response).isEqualTo(SongOfDayResponse.unlinked(appUserId));
    }

    @Test
    void getCurrentSongOfDayReturnsPendingWhenWinnerNotComputedYet() {
        SongOfDayRepository songOfDayRepository = mock(SongOfDayRepository.class);
        SpotifyAccountRepository spotifyAccountRepository = mock(SpotifyAccountRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-18T03:00:00Z"), ZoneOffset.UTC);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(spotifyAccountRepository.findAccountIdentityByAppUserId(appUserId))
                .thenReturn(Optional.of(new LinkedSpotifyAccountIdentity(appUserId, "lukerykta", "America/New_York")));
        when(songOfDayRepository.findCurrentWinner(appUserId, LocalDate.parse("2026-03-17")))
                .thenReturn(Optional.empty());

        SongOfDayService service = new SongOfDayService(songOfDayRepository, spotifyAccountRepository, clock);

        SongOfDayResponse response = service.getCurrentSongOfDay(appUserId);

        assertThat(response).isEqualTo(SongOfDayResponse.pending(appUserId));
    }
}
