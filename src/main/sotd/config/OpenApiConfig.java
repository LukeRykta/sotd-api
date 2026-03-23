package sotd.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OpenAPI/Swagger configuration for frontend and upstream integration consumers.
 */
@Configuration
public class OpenApiConfig {

    public static final String UPSTREAM_HEADER_AUTH_SCHEME = "upstreamHeaderAuth";
    public static final String UPSTREAM_QUERY_AUTH_SCHEME = "upstreamQueryAuth";

    @Bean
    OpenAPI sotdOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SOTD API")
                        .version("v1")
                        .description("User-scoped Spotify polling, top-song, listening-streaks, and shared-song API.")
                        .contact(new Contact().name("SOTD API")))
                .components(new Components()
                        .addSecuritySchemes(
                                UPSTREAM_HEADER_AUTH_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Short-lived upstream-issued JWT for server-to-server user-scoped requests.")
                        )
                        .addSecuritySchemes(
                                UPSTREAM_QUERY_AUTH_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.QUERY)
                                        .name("upstreamAuth")
                                        .description("Short-lived upstream-issued JWT for browser redirects into the Spotify connect flow.")
                        ))
                .addTagsItem(new Tag().name("top-song").description("User-scoped winner reads for profile pages."))
                .addTagsItem(new Tag().name("listening-streaks").description("User-scoped derived listening streak insights."))
                .addTagsItem(new Tag().name("our-song").description("Pairwise shared-song reads for two profile pages."))
                .addTagsItem(new Tag().name("spotify-auth").description("Spotify account linking and linked-account inspection."));
    }

    @Bean
    ServerBaseUrlCustomizer serverBaseUrlCustomizer(@Value("${sotd.openapi.server-url:}") String configuredServerUrl) {
        return (serverBaseUrl, request) -> customizeServerBaseUrl(serverBaseUrl, request, configuredServerUrl);
    }

    static String customizeServerBaseUrl(String serverBaseUrl, HttpRequest request, String configuredServerUrl) {
        if (StringUtils.hasText(configuredServerUrl)) {
            return stripTrailingSlash(configuredServerUrl);
        }

        String forwardedProto = resolveForwardedProto(request);
        if (!StringUtils.hasText(forwardedProto)) {
            return stripTrailingSlash(serverBaseUrl);
        }

        return stripTrailingSlash(UriComponentsBuilder.fromUriString(serverBaseUrl)
                .scheme(forwardedProto)
                .build(true)
                .toUriString());
    }

    static String resolveForwardedProto(HttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String xForwardedProto = headers.getFirst("X-Forwarded-Proto");
        if (StringUtils.hasText(xForwardedProto)) {
            return firstHeaderValue(xForwardedProto);
        }

        String forwarded = headers.getFirst("Forwarded");
        if (!StringUtils.hasText(forwarded)) {
            return null;
        }

        for (String part : forwarded.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.regionMatches(true, 0, "proto=", 0, 6)) {
                continue;
            }

            String value = trimmed.substring(6).trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                value = value.substring(1, value.length() - 1);
            }
            return firstHeaderValue(value);
        }

        return null;
    }

    private static String firstHeaderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int commaIndex = value.indexOf(',');
        return commaIndex >= 0 ? value.substring(0, commaIndex).trim() : value.trim();
    }

    private static String stripTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
