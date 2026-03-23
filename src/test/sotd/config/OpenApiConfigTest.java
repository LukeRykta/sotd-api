package sotd.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

class OpenApiConfigTest {

    @Test
    void openApiBeanIncludesExpectedSecuritySchemesAndMetadata() {
        OpenAPI openAPI = new OpenApiConfig().sotdOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("SOTD API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKeys(OpenApiConfig.UPSTREAM_HEADER_AUTH_SCHEME, OpenApiConfig.UPSTREAM_QUERY_AUTH_SCHEME);
        assertThat(openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.UPSTREAM_HEADER_AUTH_SCHEME).getType())
                .isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.UPSTREAM_HEADER_AUTH_SCHEME).getScheme())
                .isEqualTo("bearer");
        assertThat(openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.UPSTREAM_QUERY_AUTH_SCHEME).getIn())
                .isEqualTo(SecurityScheme.In.QUERY);
    }

    @Test
    void customizeServerBaseUrlUsesConfiguredOverrideWhenPresent() {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/openapi");

        String actual = OpenApiConfig.customizeServerBaseUrl(
                "http://internal-service:8080",
                request,
                "https://api.example.com"
        );

        assertThat(actual).isEqualTo("https://api.example.com");
    }

    @Test
    void customizeServerBaseUrlUsesForwardedProtoWhenPresent() {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/openapi");
        request.getHeaders().add("X-Forwarded-Proto", "https");

        String actual = OpenApiConfig.customizeServerBaseUrl(
                "http://internal-service:8080",
                request,
                ""
        );

        assertThat(actual).isEqualTo("https://internal-service:8080");
    }

    @Test
    void customizeServerBaseUrlParsesStandardForwardedHeader() {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/openapi");
        request.getHeaders().add("Forwarded", "for=1.2.3.4;proto=https;host=api.example.com");

        String actual = OpenApiConfig.customizeServerBaseUrl(
                "http://internal-service:8080",
                request,
                ""
        );

        assertThat(actual).isEqualTo("https://internal-service:8080");
    }
}
