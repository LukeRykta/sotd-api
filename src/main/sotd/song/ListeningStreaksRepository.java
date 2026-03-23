package sotd.song;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC reads for listening-streak insights, sourced from existing playback and daily rollup tables.
 */
@Repository
public class ListeningStreaksRepository {

    private final JdbcClient jdbcClient;

    public ListeningStreaksRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<AccountContext> findAccountContextByAppUserId(UUID appUserId) {
        return jdbcClient.sql("""
                select
                    id,
                    app_user_id,
                    spotify_user_id,
                    display_name,
                    timezone
                from spotify_account
                where app_user_id = ?
                order by updated_at desc
                limit 1
                """)
                .param(appUserId)
                .query((rs, rowNum) -> new AccountContext(
                        rs.getLong("id"),
                        rs.getObject("app_user_id", UUID.class),
                        rs.getString("spotify_user_id"),
                        rs.getString("display_name"),
                        rs.getString("timezone")
                ))
                .optional();
    }

    public List<LocalDate> findActiveListeningDays(long accountId, LocalDate windowStartLocal, LocalDate windowEndLocalExclusive) {
        return jdbcClient.sql("""
                select played_date_local
                from playback_event
                where spotify_account_id = ?
                  and played_date_local >= ?
                  and played_date_local < ?
                group by played_date_local
                order by played_date_local asc
                """)
                .params(accountId, windowStartLocal, windowEndLocalExclusive)
                .query((rs, rowNum) -> rs.getObject("played_date_local", LocalDate.class))
                .list();
    }

    public List<DailyTrackRollup> findDailyTrackRollups(long accountId, LocalDate windowStartLocal, LocalDate windowEndLocalExclusive) {
        return jdbcClient.sql("""
                select
                    spr.period_start_local,
                    spr.spotify_track_id,
                    st.name as track_name,
                    artists.artist_name,
                    st.image_url,
                    spr.play_count,
                    spr.last_played_at_utc
                from song_period_rollup spr
                join spotify_track st on st.spotify_track_id = spr.spotify_track_id
                left join lateral (
                    select string_agg(sa.name, ', ' order by sta.artist_order) as artist_name
                    from spotify_track_artist sta
                    join spotify_artist sa on sa.spotify_artist_id = sta.spotify_artist_id
                    where sta.spotify_track_id = spr.spotify_track_id
                ) artists on true
                where spr.spotify_account_id = ?
                  and spr.period_type = 'DAY'
                  and spr.period_start_local >= ?
                  and spr.period_start_local < ?
                order by spr.spotify_track_id asc, spr.period_start_local asc
                """)
                .params(accountId, windowStartLocal, windowEndLocalExclusive)
                .query((rs, rowNum) -> new DailyTrackRollup(
                        rs.getObject("period_start_local", LocalDate.class),
                        rs.getString("spotify_track_id"),
                        rs.getString("track_name"),
                        rs.getString("artist_name"),
                        rs.getString("image_url"),
                        rs.getInt("play_count"),
                        rs.getTimestamp("last_played_at_utc").toInstant()
                ))
                .list();
    }

    public List<DailyArtistRollup> findDailyArtistRollups(long accountId, LocalDate windowStartLocal, LocalDate windowEndLocalExclusive) {
        return jdbcClient.sql("""
                select
                    spr.period_start_local,
                    sa.spotify_artist_id,
                    sa.name as artist_name,
                    sum(spr.play_count) as play_count,
                    max(spr.last_played_at_utc) as last_played_at_utc
                from song_period_rollup spr
                join spotify_track_artist sta on sta.spotify_track_id = spr.spotify_track_id
                join spotify_artist sa on sa.spotify_artist_id = sta.spotify_artist_id
                where spr.spotify_account_id = ?
                  and spr.period_type = 'DAY'
                  and spr.period_start_local >= ?
                  and spr.period_start_local < ?
                group by spr.period_start_local, sa.spotify_artist_id, sa.name
                order by sa.spotify_artist_id asc, spr.period_start_local asc
                """)
                .params(accountId, windowStartLocal, windowEndLocalExclusive)
                .query((rs, rowNum) -> new DailyArtistRollup(
                        rs.getObject("period_start_local", LocalDate.class),
                        rs.getString("spotify_artist_id"),
                        rs.getString("artist_name"),
                        rs.getLong("play_count"),
                        rs.getTimestamp("last_played_at_utc").toInstant()
                ))
                .list();
    }

    public record AccountContext(
            long accountId,
            UUID appUserId,
            String spotifyUserId,
            String displayName,
            String timezone
    ) {
    }

    public record DailyTrackRollup(
            LocalDate dateLocal,
            String spotifyTrackId,
            String trackName,
            String artistName,
            String imageUrl,
            int playCount,
            Instant lastPlayedAtUtc
    ) {
    }

    public record DailyArtistRollup(
            LocalDate dateLocal,
            String spotifyArtistId,
            String artistName,
            long playCount,
            Instant lastPlayedAtUtc
    ) {
    }
}
