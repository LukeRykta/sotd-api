package sotd.spotify.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sotd.spotify.SpotifyLinkedAccountView;
import sotd.spotify.client.SpotifyCurrentUserProfile;

@Repository
/**
 * JDBC persistence for linked Spotify accounts.
 *
 * <p>This repository models a one-to-one relationship between an upstream application user UUID and a
 * linked Spotify account row used for polling and read APIs.
 */
public class SpotifyAccountRepository {

    private final JdbcClient jdbcClient;

    public SpotifyAccountRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Upserts the linked Spotify account after a successful OAuth callback.
     */
    @Transactional
    public void saveOrUpdate(
            UUID appUserId,
            SpotifyCurrentUserProfile userProfile,
            byte[] encryptedRefreshToken,
            String scope,
            Instant accessTokenExpiresAt,
            Instant lastRefreshAt,
            String timezone
    ) {
        // Keep the UUID-to-Spotify-account mapping one-to-one if the user reconnects with a different account.
        jdbcClient.sql("""
                update spotify_account
                set app_user_id = null,
                    status = 'DISCONNECTED',
                    updated_at = current_timestamp
                where app_user_id = ?
                  and spotify_user_id <> ?
                """)
                .params(appUserId, userProfile.id())
                .update();

        jdbcClient.sql("""
                insert into spotify_account (
                    app_user_id,
                    spotify_user_id,
                    display_name,
                    scope,
                    refresh_token_encrypted,
                    access_token_expires_at,
                    last_refresh_at,
                    timezone,
                    status,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', current_timestamp)
                on conflict (spotify_user_id) do update
                set app_user_id = excluded.app_user_id,
                    display_name = excluded.display_name,
                    scope = excluded.scope,
                    refresh_token_encrypted = excluded.refresh_token_encrypted,
                    access_token_expires_at = excluded.access_token_expires_at,
                    last_refresh_at = excluded.last_refresh_at,
                    timezone = excluded.timezone,
                    status = 'ACTIVE',
                    updated_at = current_timestamp
                """)
                .params(
                        appUserId,
                        userProfile.id(),
                        userProfile.displayName(),
                        scope,
                        encryptedRefreshToken,
                        Timestamp.from(accessTokenExpiresAt),
                        Timestamp.from(lastRefreshAt),
                        timezone
                )
                .update();
    }

    /**
     * Returns the linked account row for a specific application user.
     */
    public Optional<SpotifyLinkedAccountView> findByAppUserId(UUID appUserId) {
        return jdbcClient.sql("""
                select
                    app_user_id,
                    spotify_user_id,
                    display_name,
                    scope,
                    status,
                    timezone,
                    access_token_expires_at,
                    created_at,
                    updated_at
                from spotify_account
                where app_user_id = ?
                """)
                .param(appUserId)
                .query((rs, rowNum) -> new SpotifyLinkedAccountView(
                        rs.getObject("app_user_id", UUID.class),
                        rs.getString("spotify_user_id"),
                        rs.getString("display_name"),
                        rs.getString("scope"),
                        rs.getString("status"),
                        rs.getString("timezone"),
                        toInstant(rs.getTimestamp("access_token_expires_at")),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at"))
                ))
                .optional();
    }

    /**
     * Returns the linked account identity for a specific application user.
     */
    public Optional<LinkedSpotifyAccountIdentity> findAccountIdentityByAppUserId(UUID appUserId) {
        return jdbcClient.sql("""
                select
                    app_user_id,
                    spotify_user_id,
                    timezone
                from spotify_account
                where app_user_id = ?
                """)
                .param(appUserId)
                .query((rs, rowNum) -> new LinkedSpotifyAccountIdentity(
                        rs.getObject("app_user_id", UUID.class),
                        rs.getString("spotify_user_id"),
                        rs.getString("timezone")
                ))
                .optional();
    }

    /**
     * Returns active accounts that are eligible for background Spotify polling.
     */
    public List<SpotifyPollingAccount> findActiveAccountsForPolling() {
        return jdbcClient.sql("""
                select
                    id,
                    spotify_user_id,
                    refresh_token_encrypted,
                    access_token_expires_at,
                    last_recently_played_cursor_ms,
                    timezone
                from spotify_account
                where status = 'ACTIVE'
                order by updated_at desc
                """)
                .query((rs, rowNum) -> new SpotifyPollingAccount(
                        rs.getLong("id"),
                        rs.getString("spotify_user_id"),
                        rs.getBytes("refresh_token_encrypted"),
                        toInstant(rs.getTimestamp("access_token_expires_at")),
                        rs.getObject("last_recently_played_cursor_ms", Long.class),
                        rs.getString("timezone")
                ))
                .list();
    }

    /**
     * Persists new token metadata after a refresh-token exchange.
     */
    public void updateTokenState(
            long accountId,
            byte[] encryptedRefreshToken,
            Instant accessTokenExpiresAt,
            Instant lastRefreshAt
    ) {
        jdbcClient.sql("""
                update spotify_account
                set refresh_token_encrypted = ?,
                    access_token_expires_at = ?,
                    last_refresh_at = ?,
                    status = 'ACTIVE',
                    updated_at = current_timestamp
                where id = ?
                """)
                .params(
                        encryptedRefreshToken,
                        Timestamp.from(accessTokenExpiresAt),
                        Timestamp.from(lastRefreshAt),
                        accountId
                )
                .update();
    }

    /**
     * Marks the polling checkpoint after a successful recently-played sync.
     */
    public void updatePollingCheckpoint(long accountId, Instant lastSuccessfulPollAt, Long lastRecentlyPlayedCursorMs) {
        jdbcClient.sql("""
                update spotify_account
                set last_successful_poll_at = ?,
                    last_recently_played_cursor_ms = coalesce(?, last_recently_played_cursor_ms),
                    updated_at = current_timestamp
                where id = ?
                """)
                .params(
                        Timestamp.from(lastSuccessfulPollAt),
                        lastRecentlyPlayedCursorMs,
                        accountId
                )
                .update();
    }

    /**
     * Marks a linked account as requiring reauthorization.
     */
    public void markReauthRequired(long accountId) {
        jdbcClient.sql("""
                update spotify_account
                set status = 'REAUTH_REQUIRED',
                    updated_at = current_timestamp
                where id = ?
                """)
                .param(accountId)
                .update();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    public record LinkedSpotifyAccountIdentity(
            UUID appUserId,
            String spotifyUserId,
            String timezone
    ) {
    }
}
