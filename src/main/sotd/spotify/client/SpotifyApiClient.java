package sotd.spotify.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SpotifyApiClient {

    private final RestClient spotifyApiRestClient;

    public SpotifyApiClient(@Qualifier("spotifyApiRestClient") RestClient spotifyApiRestClient) {
        this.spotifyApiRestClient = spotifyApiRestClient;
    }

    public SpotifyCurrentUserProfile getCurrentUserProfile(String accessToken) {
        SpotifyCurrentUserProfile response = spotifyApiRestClient.get()
                .uri("/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(SpotifyCurrentUserProfile.class);

        if (response == null || response.id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Spotify profile response was empty.");
        }
        return response;
    }
}
