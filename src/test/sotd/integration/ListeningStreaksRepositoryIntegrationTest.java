package sotd.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import sotd.song.ListeningStreaksRepository;
import sotd.support.PostgresJdbcIntegrationTestSupport;

@Transactional
class ListeningStreaksRepositoryIntegrationTest extends PostgresJdbcIntegrationTestSupport {

    @Autowired
    private ListeningStreaksRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void repositoryReturnsAccountContextAndDailyRowsForStreakComputation() {
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        long accountId = insertSpotifyAccount(appUserId, "lukerykta", "Luke");
        insertSpotifyTrack("track-1", "Track One");
        insertSpotifyTrack("track-2", "Track Two");
        insertSpotifyArtist("artist-1", "Artist One");
        insertSpotifyArtist("artist-2", "Artist Two");
        insertTrackArtist("track-1", "artist-1", 0);
        insertTrackArtist("track-2", "artist-1", 0);
        insertTrackArtist("track-2", "artist-2", 1);

        insertPlaybackEvent(accountId, "track-1", "2026-03-14T10:00:00Z", LocalDate.parse("2026-03-14"));
        insertPlaybackEvent(accountId, "track-2", "2026-03-15T11:00:00Z", LocalDate.parse("2026-03-15"));

        insertSongPeriodRollup(accountId, "track-1", LocalDate.parse("2026-03-14"), 2, "2026-03-14T10:00:00Z");
        insertSongPeriodRollup(accountId, "track-2", LocalDate.parse("2026-03-15"), 3, "2026-03-15T11:00:00Z");

        ListeningStreaksRepository.AccountContext accountContext = repository.findAccountContextByAppUserId(appUserId).orElseThrow();

        assertThat(accountContext.accountId()).isEqualTo(accountId);
        assertThat(accountContext.spotifyUserId()).isEqualTo("lukerykta");

        assertThat(repository.findActiveListeningDays(accountId, LocalDate.parse("2026-03-14"), LocalDate.parse("2026-03-16")))
                .containsExactly(LocalDate.parse("2026-03-14"), LocalDate.parse("2026-03-15"));

        assertThat(repository.findDailyTrackRollups(accountId, LocalDate.parse("2026-03-14"), LocalDate.parse("2026-03-16")))
                .hasSize(2)
                .extracting(ListeningStreaksRepository.DailyTrackRollup::trackName)
                .containsExactly("Track One", "Track Two");

        assertThat(repository.findDailyArtistRollups(accountId, LocalDate.parse("2026-03-14"), LocalDate.parse("2026-03-16")))
                .hasSize(3)
                .extracting(ListeningStreaksRepository.DailyArtistRollup::artistName)
                .containsExactly("Artist One", "Artist One", "Artist Two");
    }

    private long insertSpotifyAccount(UUID appUserId, String spotifyUserId, String displayName) {
        return jdbcClient.sql("""
                insert into spotify_account (
                    app_user_id,
                    spotify_user_id,
                    display_name,
                    scope,
                    refresh_token_encrypted,
                    timezone,
                    status
                ) values (?, ?, ?, '', cast(E'\\\\x01' as bytea), 'America/Chicago', 'ACTIVE')
                returning id
                """)
                .params(appUserId, spotifyUserId, displayName)
                .query(Long.class)
                .single();
    }

    private void insertSpotifyTrack(String spotifyTrackId, String name) {
        jdbcClient.sql("""
                insert into spotify_track (
                    spotify_track_id,
                    name,
                    duration_ms
                ) values (?, ?, 180000)
                """)
                .params(spotifyTrackId, name)
                .update();
    }

    private void insertSpotifyArtist(String spotifyArtistId, String name) {
        jdbcClient.sql("""
                insert into spotify_artist (
                    spotify_artist_id,
                    name
                ) values (?, ?)
                """)
                .params(spotifyArtistId, name)
                .update();
    }

    private void insertTrackArtist(String spotifyTrackId, String spotifyArtistId, int artistOrder) {
        jdbcClient.sql("""
                insert into spotify_track_artist (
                    spotify_track_id,
                    spotify_artist_id,
                    artist_order
                ) values (?, ?, ?)
                """)
                .params(spotifyTrackId, spotifyArtistId, artistOrder)
                .update();
    }

    private void insertPlaybackEvent(long accountId, String spotifyTrackId, String playedAtUtc, LocalDate playedDateLocal) {
        jdbcClient.sql("""
                insert into playback_event (
                    spotify_account_id,
                    spotify_track_id,
                    played_at_utc,
                    played_date_local
                ) values (?, ?, ?::timestamptz, ?)
                """)
                .params(accountId, spotifyTrackId, playedAtUtc, playedDateLocal)
                .update();
    }

    private void insertSongPeriodRollup(
            long accountId,
            String spotifyTrackId,
            LocalDate periodStartLocal,
            int playCount,
            String lastPlayedAtUtc
    ) {
        long totalDurationMs = 180000L * playCount;
        jdbcClient.sql("""
                insert into song_period_rollup (
                    spotify_account_id,
                    period_type,
                    period_start_local,
                    spotify_track_id,
                    play_count,
                    total_duration_ms,
                    last_played_at_utc
                ) values (?, 'DAY', ?, ?, ?, ?, ?::timestamptz)
                """)
                .params(accountId, periodStartLocal, spotifyTrackId, playCount, totalDurationMs, lastPlayedAtUtc)
                .update();
    }
}
