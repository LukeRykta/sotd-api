package sotd.spotify;

import java.time.Instant;
import java.util.UUID;

public record SpotifyLinkedAccountView(
        UUID appUserId,
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
