package sotd.spotify;

import java.time.Instant;
import java.util.UUID;

public record SpotifyConnectionResponse(
        String status,
        UUID appUserId,
        String spotifyUserId,
        String displayName,
        String grantedScope,
        Instant accessTokenExpiresAt
) {
}
