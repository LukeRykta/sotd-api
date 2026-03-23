package sotd.auth;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Enforces that user-scoped routes are called with a valid upstream-issued auth token.
 */
@Component
public class UpstreamAuthInterceptor implements HandlerInterceptor {

    static final String VERIFIED_APP_USER_ID_ATTRIBUTE = "verifiedAppUserId";
    private static final Logger log = LoggerFactory.getLogger(UpstreamAuthInterceptor.class);

    private final UpstreamAuthProperties upstreamAuthProperties;
    private final UpstreamRequestTokenService upstreamRequestTokenService;

    public UpstreamAuthInterceptor(
            UpstreamAuthProperties upstreamAuthProperties,
            UpstreamRequestTokenService upstreamRequestTokenService
    ) {
        this.upstreamAuthProperties = upstreamAuthProperties;
        this.upstreamRequestTokenService = upstreamRequestTokenService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        if (!upstreamAuthProperties.isEnabled()) {
            return true;
        }

        Map<String, String> uriVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVariables == null || !uriVariables.containsKey("appUserId")) {
            return true;
        }

        UUID pathAppUserId = parsePathUserId(uriVariables.get("appUserId"));
        String tokenSource = resolveTokenSource(request);
        String token = resolveToken(request, tokenSource);
        UpstreamRequestTokenService.VerifiedUpstreamRequest verifiedRequest;
        try {
            verifiedRequest = upstreamRequestTokenService.verify(token);
        }
        catch (ResponseStatusException ex) {
            log.warn(
                    "Rejected upstream-auth request {} {} for appUserId={} via {} with status {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    pathAppUserId,
                    tokenSource,
                    ex.getStatusCode().value(),
                    ex.getReason()
            );
            throw ex;
        }

        if (!verifiedRequest.appUserId().equals(pathAppUserId)) {
            log.warn(
                    "Rejected upstream-auth request {} {} because token subject {} did not match path appUserId {}.",
                    request.getMethod(),
                    request.getRequestURI(),
                    verifiedRequest.appUserId(),
                    pathAppUserId
            );
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Upstream authorization token does not match the requested user.");
        }

        request.setAttribute(VERIFIED_APP_USER_ID_ATTRIBUTE, verifiedRequest.appUserId());
        return true;
    }

    private String resolveToken(jakarta.servlet.http.HttpServletRequest request, String tokenSource) {
        String headerToken = request.getHeader(upstreamAuthProperties.getHeaderName());
        if ("authorization-header".equals(tokenSource) && StringUtils.hasText(headerToken)) {
            return extractBearerToken(headerToken);
        }
        if ("spotify-connect-query-parameter".equals(tokenSource)) {
            return request.getParameter(upstreamAuthProperties.getQueryParameterName());
        }
        return null;
    }

    private String resolveTokenSource(jakarta.servlet.http.HttpServletRequest request) {
        String headerToken = request.getHeader(upstreamAuthProperties.getHeaderName());
        if (StringUtils.hasText(headerToken)) {
            return "authorization-header";
        }
        if (isBrowserConnectRequest(request)) {
            return "spotify-connect-query-parameter";
        }
        return "none";
    }

    private boolean isBrowserConnectRequest(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI() != null
                && request.getRequestURI().endsWith("/spotify/connect");
    }

    private static String extractBearerToken(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }
        if (headerValue.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return headerValue.substring(7).trim();
        }
        return headerValue;
    }

    private static UUID parsePathUserId(String value) {
        try {
            return UUID.fromString(value);
        }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested user id is invalid.");
        }
    }
}
