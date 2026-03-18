package sotd.spotify;

import java.time.Instant;

public record SpotifyLinkedAccountView(
        String spotifyUserId,
        String displayName,
        String scope,
        String status,
        String timezone,
        Instant accessTokenExpiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
