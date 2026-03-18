package sotd.spotify;

import java.time.Instant;

public record SpotifyConnectionResponse(
        String status,
        String spotifyUserId,
        String displayName,
        String grantedScope,
        Instant accessTokenExpiresAt
) {
}
