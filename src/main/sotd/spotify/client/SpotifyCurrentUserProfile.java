package sotd.spotify.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpotifyCurrentUserProfile(
        String id,
        @JsonProperty("display_name") String displayName
) {
}
