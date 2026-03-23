package sotd.song;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Queries the highest-ranked song for one user by aggregating daily rollups into the requested window.
 */
@Repository
public class TopSongRepository {

    static final String TIE_BREAK_RULE = "PLAY_COUNT_THEN_LAST_PLAYED_THEN_TRACK_ID";

    private final JdbcClient jdbcClient;

    public TopSongRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<TopSongWinnerView> findTopSong(
            UUID appUserId,
            SongPeriodType periodType,
            LocalDate periodStartLocal,
            LocalDate periodEndExclusive
    ) {
        return jdbcClient.sql("""
                with account as (
                    select
                        id,
                        app_user_id,
                        spotify_user_id,
                        display_name,
                        timezone
                    from spotify_account
                    where app_user_id = ?
                ),
                aggregated_rollup as (
                    select
                        spotify_track_id,
                        sum(play_count) as play_count,
                        max(last_played_at_utc) as last_played_at_utc
                    from song_period_rollup
                    where spotify_account_id = (select id from account)
                      and period_type = 'DAY'
                      and period_start_local >= ?
                      and period_start_local < ?
                    group by spotify_track_id
                )
                select
                    a.app_user_id,
                    a.spotify_user_id,
                    a.display_name,
                    a.timezone,
                    ar.spotify_track_id,
                    st.name as track_name,
                    artists.artist_name,
                    st.image_url,
                    ar.play_count
                from aggregated_rollup ar
                join spotify_track st on st.spotify_track_id = ar.spotify_track_id
                left join lateral (
                    select string_agg(sa.name, ', ' order by sta.artist_order) as artist_name
                    from spotify_track_artist sta
                    join spotify_artist sa on sa.spotify_artist_id = sta.spotify_artist_id
                    where sta.spotify_track_id = ar.spotify_track_id
                ) artists on true
                cross join account a
                order by
                    ar.play_count desc,
                    ar.last_played_at_utc desc,
                    ar.spotify_track_id asc
                limit 1
                """)
                .params(appUserId, periodStartLocal, periodEndExclusive)
                .query((rs, rowNum) -> new TopSongWinnerView(
                        rs.getObject("app_user_id", UUID.class),
                        rs.getString("spotify_user_id"),
                        rs.getString("display_name"),
                        rs.getString("timezone"),
                        periodType,
                        periodStartLocal,
                        rs.getString("spotify_track_id"),
                        rs.getString("track_name"),
                        rs.getString("artist_name"),
                        rs.getString("image_url"),
                        rs.getInt("play_count"),
                        TIE_BREAK_RULE
                ))
                .optional();
    }
}
