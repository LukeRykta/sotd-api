package sotd.song;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sotd.song.ListeningStreaksRepository.AccountContext;
import sotd.song.ListeningStreaksRepository.DailyArtistRollup;
import sotd.song.ListeningStreaksRepository.DailyTrackRollup;

class ListeningStreaksServiceTest {

    @Test
    void getListeningStreaksReturnsComputedInsights() {
        ListeningStreaksRepository repository = mock(ListeningStreaksRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-18T17:00:00Z"), ZoneOffset.UTC);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AccountContext accountContext = new AccountContext(7L, appUserId, "lukerykta", "Luke", "America/Chicago");

        when(repository.findAccountContextByAppUserId(appUserId)).thenReturn(Optional.of(accountContext));
        when(repository.findActiveListeningDays(7L, LocalDate.parse("2025-12-19"), LocalDate.parse("2026-03-19")))
                .thenReturn(List.of(
                        LocalDate.parse("2026-03-14"),
                        LocalDate.parse("2026-03-15"),
                        LocalDate.parse("2026-03-16"),
                        LocalDate.parse("2026-03-18")
                ));
        when(repository.findDailyTrackRollups(7L, LocalDate.parse("2025-12-19"), LocalDate.parse("2026-03-19")))
                .thenReturn(List.of(
                        new DailyTrackRollup(LocalDate.parse("2026-03-14"), "track-a", "Track A", "Artist A", "https://img.test/track-a.jpg", 2, Instant.parse("2026-03-14T12:00:00Z")),
                        new DailyTrackRollup(LocalDate.parse("2026-03-15"), "track-a", "Track A", "Artist A", "https://img.test/track-a.jpg", 3, Instant.parse("2026-03-15T12:00:00Z")),
                        new DailyTrackRollup(LocalDate.parse("2026-03-16"), "track-a", "Track A", "Artist A", "https://img.test/track-a.jpg", 4, Instant.parse("2026-03-16T12:00:00Z")),
                        new DailyTrackRollup(LocalDate.parse("2026-03-16"), "track-b", "Track B", "Artist B", "https://img.test/track-b.jpg", 5, Instant.parse("2026-03-16T13:00:00Z")),
                        new DailyTrackRollup(LocalDate.parse("2026-03-18"), "track-b", "Track B", "Artist B", "https://img.test/track-b.jpg", 6, Instant.parse("2026-03-18T12:00:00Z"))
                ));
        when(repository.findDailyArtistRollups(7L, LocalDate.parse("2025-12-19"), LocalDate.parse("2026-03-19")))
                .thenReturn(List.of(
                        new DailyArtistRollup(LocalDate.parse("2026-03-14"), "artist-a", "Artist A", 2, Instant.parse("2026-03-14T12:00:00Z")),
                        new DailyArtistRollup(LocalDate.parse("2026-03-15"), "artist-a", "Artist A", 3, Instant.parse("2026-03-15T12:00:00Z")),
                        new DailyArtistRollup(LocalDate.parse("2026-03-16"), "artist-a", "Artist A", 9, Instant.parse("2026-03-16T13:00:00Z")),
                        new DailyArtistRollup(LocalDate.parse("2026-03-18"), "artist-b", "Artist B", 6, Instant.parse("2026-03-18T12:00:00Z"))
                ));

        ListeningStreaksService service = new ListeningStreaksService(repository, clock);

        ListeningStreaksResponse response = service.getListeningStreaks(appUserId, 90, 2);

        assertThat(response.status()).isEqualTo("ready");
        assertThat(response.windowStartLocal()).isEqualTo(LocalDate.parse("2025-12-19"));
        assertThat(response.windowEndLocalExclusive()).isEqualTo(LocalDate.parse("2026-03-19"));
        assertThat(response.activeDayStreak().currentDays()).isEqualTo(1);
        assertThat(response.activeDayStreak().longestDays()).isEqualTo(3);
        assertThat(response.activeDayStreak().includesToday()).isTrue();
        assertThat(response.featuredTrackStreak().spotifyTrackId()).isEqualTo("track-a");
        assertThat(response.featuredTrackStreak().artistName()).isEqualTo("Artist A");
        assertThat(response.featuredTrackStreak().imageUrl()).isEqualTo("https://img.test/track-a.jpg");
        assertThat(response.featuredTrackStreak().days()).isEqualTo(3);
        assertThat(response.featuredTrackStreak().totalPlaysAcrossStreak()).isEqualTo(9L);
        assertThat(response.featuredArtistStreak().spotifyArtistId()).isEqualTo("artist-a");
        assertThat(response.heaviestListeningDay().dateLocal()).isEqualTo(LocalDate.parse("2026-03-16"));
        assertThat(response.heaviestListeningDay().totalPlays()).isEqualTo(9L);
        assertThat(response.repeatOffenderTracks()).hasSize(2);
        assertThat(response.repeatOffenderTracks().get(0).spotifyTrackId()).isEqualTo("track-a");
        assertThat(response.repeatOffenderTracks().get(0).artistName()).isEqualTo("Artist A");
        assertThat(response.repeatOffenderTracks().get(0).imageUrl()).isEqualTo("https://img.test/track-a.jpg");
        assertThat(response.repeatOffenderTracks().get(0).activeDays()).isEqualTo(3);
    }

    @Test
    void getListeningStreaksReturnsUnlinkedWhenNoSpotifyAccountExists() {
        ListeningStreaksRepository repository = mock(ListeningStreaksRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-18T17:00:00Z"), ZoneOffset.UTC);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(repository.findAccountContextByAppUserId(appUserId)).thenReturn(Optional.empty());

        ListeningStreaksService service = new ListeningStreaksService(repository, clock);

        ListeningStreaksResponse response = service.getListeningStreaks(appUserId, 90, 5);

        assertThat(response.status()).isEqualTo("unlinked");
        assertThat(response.windowStartLocal()).isNull();
        assertThat(response.repeatOffenderTracks()).isEmpty();
    }

    @Test
    void getListeningStreaksReturnsNoDataWhenNoPlaybackExistsInWindow() {
        ListeningStreaksRepository repository = mock(ListeningStreaksRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-18T17:00:00Z"), ZoneOffset.UTC);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AccountContext accountContext = new AccountContext(7L, appUserId, "lukerykta", "Luke", "America/Chicago");

        when(repository.findAccountContextByAppUserId(appUserId)).thenReturn(Optional.of(accountContext));
        when(repository.findActiveListeningDays(7L, LocalDate.parse("2025-12-19"), LocalDate.parse("2026-03-19")))
                .thenReturn(List.of());

        ListeningStreaksService service = new ListeningStreaksService(repository, clock);

        ListeningStreaksResponse response = service.getListeningStreaks(appUserId, 90, 5);

        assertThat(response.status()).isEqualTo("no-data");
        assertThat(response.spotifyUserId()).isEqualTo("lukerykta");
        assertThat(response.windowStartLocal()).isEqualTo(LocalDate.parse("2025-12-19"));
        assertThat(response.windowEndLocalExclusive()).isEqualTo(LocalDate.parse("2026-03-19"));
        assertThat(response.activeDayStreak()).isNull();
        assertThat(response.repeatOffenderTracks()).isEmpty();
    }
}
