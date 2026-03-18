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

class TopSongServiceTest {

    @Test
    void getTopSongReturnsCurrentWinnerForRequestedAppUserAndPeriod() {
        TopSongRepository topSongRepository = mock(TopSongRepository.class);
        SpotifyAccountRepository spotifyAccountRepository = mock(SpotifyAccountRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-18T03:00:00Z"), ZoneOffset.UTC);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(spotifyAccountRepository.findAccountIdentityByAppUserId(appUserId))
                .thenReturn(Optional.of(new LinkedSpotifyAccountIdentity(appUserId, "lukerykta", "America/New_York")));
        when(topSongRepository.findTopSong(
                appUserId,
                SongPeriodType.WEEK,
                LocalDate.parse("2026-03-16"),
                LocalDate.parse("2026-03-23")
        )).thenReturn(Optional.of(new TopSongWinnerView(
                        appUserId,
                        "lukerykta",
                        "Luke",
                        "America/New_York",
                        SongPeriodType.WEEK,
                        LocalDate.parse("2026-03-16"),
                        "track-1",
                        "Track Name",
                        4,
                        TopSongRepository.TIE_BREAK_RULE
                )));

        TopSongService service = new TopSongService(topSongRepository, spotifyAccountRepository, clock);

        TopSongResponse response = service.getTopSong(appUserId, SongPeriodType.WEEK);

        assertThat(response.status()).isEqualTo("ready");
        assertThat(response.appUserId()).isEqualTo(appUserId);
        assertThat(response.spotifyUserId()).isEqualTo("lukerykta");
        assertThat(response.periodType()).isEqualTo(SongPeriodType.WEEK);
        assertThat(response.periodStartLocal()).isEqualTo(LocalDate.parse("2026-03-16"));
        assertThat(response.trackName()).isEqualTo("Track Name");
        assertThat(response.playCount()).isEqualTo(4);
    }

    @Test
    void getTopSongReturnsUnlinkedWhenNoAccountIsLinked() {
        TopSongRepository topSongRepository = mock(TopSongRepository.class);
        SpotifyAccountRepository spotifyAccountRepository = mock(SpotifyAccountRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-18T03:00:00Z"), ZoneOffset.UTC);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(spotifyAccountRepository.findAccountIdentityByAppUserId(appUserId)).thenReturn(Optional.empty());

        TopSongService service = new TopSongService(topSongRepository, spotifyAccountRepository, clock);

        TopSongResponse response = service.getTopSong(appUserId, SongPeriodType.YEAR);

        assertThat(response).isEqualTo(TopSongResponse.unlinked(appUserId, SongPeriodType.YEAR, null));
    }

    @Test
    void getTopSongReturnsPendingWhenWinnerNotComputedYet() {
        TopSongRepository topSongRepository = mock(TopSongRepository.class);
        SpotifyAccountRepository spotifyAccountRepository = mock(SpotifyAccountRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-18T03:00:00Z"), ZoneOffset.UTC);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(spotifyAccountRepository.findAccountIdentityByAppUserId(appUserId))
                .thenReturn(Optional.of(new LinkedSpotifyAccountIdentity(appUserId, "lukerykta", "America/New_York")));
        when(topSongRepository.findTopSong(
                appUserId,
                SongPeriodType.MONTH,
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2026-04-01")
        ))
                .thenReturn(Optional.empty());

        TopSongService service = new TopSongService(topSongRepository, spotifyAccountRepository, clock);

        TopSongResponse response = service.getTopSong(appUserId, SongPeriodType.MONTH);

        assertThat(response).isEqualTo(TopSongResponse.pending(
                appUserId,
                SongPeriodType.MONTH,
                LocalDate.parse("2026-03-01")
        ));
    }
}
