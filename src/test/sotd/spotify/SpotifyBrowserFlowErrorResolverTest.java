package sotd.spotify;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class SpotifyBrowserFlowErrorResolverTest {

    @Test
    void rendersFriendlyHtmlForConnectUnauthorizedError() throws Exception {
        SpotifyProperties properties = new SpotifyProperties();
        properties.setCallbackFrontendRedirectUri(java.net.URI.create("https://app.example.com/"));
        SpotifyBrowserFlowErrorResolver resolver = new SpotifyBrowserFlowErrorResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/11111111-1111-1111-1111-111111111111/spotify/connect");
        MockHttpServletResponse response = new MockHttpServletResponse();

        resolver.resolveException(
                request,
                response,
                null,
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing upstream authorization token.")
        );

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("Spotify Link Expired");
        assertThat(response.getContentAsString()).contains("This Spotify connect link is no longer valid.");
        assertThat(response.getContentAsString()).contains("Missing upstream authorization token.");
        assertThat(response.getContentAsString()).contains("Return to app");
        assertThat(response.getContentAsString()).contains("https://app.example.com/");
    }

    @Test
    void ignoresNonBrowserRoutes() throws Exception {
        SpotifyBrowserFlowErrorResolver resolver = new SpotifyBrowserFlowErrorResolver(new SpotifyProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/11111111-1111-1111-1111-111111111111/top-song");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object result = resolver.resolveException(
                request,
                response,
                null,
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing upstream authorization token.")
        );

        assertThat(result).isNull();
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void leavesCallbackExceptionForExistingRedirectAdvice() throws Exception {
        SpotifyBrowserFlowErrorResolver resolver = new SpotifyBrowserFlowErrorResolver(new SpotifyProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/spotify/callback");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object result = resolver.resolveException(
                request,
                response,
                null,
                SpotifyCallbackException.invalidState("Spotify callback state is invalid or expired.")
        );

        assertThat(result).isNull();
        assertThat(response.getContentAsString()).isEmpty();
    }
}
