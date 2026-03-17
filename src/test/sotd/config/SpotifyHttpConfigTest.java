package sotd.config;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import sotd.spotify.SpotifyProperties;

class SpotifyHttpConfigTest {

    private final SpotifyHttpConfig config = new SpotifyHttpConfig();

    @Test
    void spotifyApiRestClientUsesSpotifyApiBaseUrlAndJsonAcceptHeader() {
        SpotifyProperties properties = new SpotifyProperties();
        properties.setBaseUrl(URI.create("https://api.spotify.test/v1"));

        RestClient.Builder builder = config.restClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = config.spotifyApiRestClient(builder, properties);

        server.expect(requestTo("https://api.spotify.test/v1/me"))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess());

        client.get().uri("/me").retrieve().toBodilessEntity();

        server.verify();
    }

    @Test
    void spotifyAccountsRestClientUsesSpotifyAccountsBaseUrlAndJsonAcceptHeader() {
        SpotifyProperties properties = new SpotifyProperties();
        properties.setAccountsBaseUrl(URI.create("https://accounts.spotify.test"));

        RestClient.Builder builder = config.restClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = config.spotifyAccountsRestClient(builder, properties);

        server.expect(requestTo("https://accounts.spotify.test/api/token"))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess());

        client.post().uri("/api/token").retrieve().toBodilessEntity();

        server.verify();
    }
}
