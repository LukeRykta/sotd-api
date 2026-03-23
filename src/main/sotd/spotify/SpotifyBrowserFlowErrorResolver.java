package sotd.spotify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

/**
 * Renders a user-friendly HTML error page for browser-facing Spotify auth routes.
 *
 * <p>This avoids Spring Boot's default white-label page for `/spotify/connect` failures while leaving
 * JSON/API routes and the normal callback redirect handling unchanged.
 */
@Component
public class SpotifyBrowserFlowErrorResolver extends AbstractHandlerExceptionResolver {

    private static final Logger log = LoggerFactory.getLogger(SpotifyBrowserFlowErrorResolver.class);
    private final SpotifyProperties spotifyProperties;

    public SpotifyBrowserFlowErrorResolver(SpotifyProperties spotifyProperties) {
        this.spotifyProperties = spotifyProperties;
        setOrder(Ordered.LOWEST_PRECEDENCE);
    }

    @Override
    protected ModelAndView doResolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        if (!isSpotifyBrowserFlow(request) || ex instanceof SpotifyCallbackException || response.isCommitted()) {
            return null;
        }

        BrowserFlowErrorView errorView = describeError(request, ex);
        log.warn(
                "Rendering Spotify browser-flow error page for {} {} with status {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                errorView.status().value(),
                StringUtils.hasText(errorView.detail()) ? errorView.detail() : errorView.message()
        );
        response.setStatus(errorView.status().value());
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        try {
            response.getWriter().write(renderHtml(errorView));
            response.getWriter().flush();
            return new ModelAndView();
        }
        catch (IOException writeFailure) {
            log.warn("Failed to render Spotify browser-flow error page.", writeFailure);
            return null;
        }
    }

    private BrowserFlowErrorView describeError(HttpServletRequest request, Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String detail = null;

        if (ex instanceof ResponseStatusException responseStatusException) {
            status = HttpStatus.valueOf(responseStatusException.getStatusCode().value());
            if (StringUtils.hasText(responseStatusException.getReason())) {
                detail = responseStatusException.getReason();
            }
        }

        boolean connectRoute = isConnectRoute(request);
        String title;
        String message;
        String guidance;

        if (connectRoute && (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN)) {
            title = "Spotify Link Expired";
            message = "This Spotify connect link is no longer valid.";
            guidance = "Return to the app and start the Spotify connect flow again.";
        }
        else if (connectRoute) {
            title = "Spotify Connect Could Not Start";
            message = "The app could not send you to Spotify right now.";
            guidance = "Return to the app and try starting the Spotify connect flow again.";
        }
        else {
            title = "Spotify Connect Could Not Finish";
            message = "Spotify sign-in finished, but the app could not complete the link.";
            guidance = "Return to the app and try linking Spotify again.";
        }

        return new BrowserFlowErrorView(
                title,
                message,
                guidance,
                detail,
                status,
                spotifyProperties.getCallbackFrontendRedirectUri()
        );
    }

    private String renderHtml(BrowserFlowErrorView errorView) {
        String returnHref = HtmlUtils.htmlEscape(errorView.returnUri().toString());
        String title = HtmlUtils.htmlEscape(errorView.title());
        String message = HtmlUtils.htmlEscape(errorView.message());
        String guidance = HtmlUtils.htmlEscape(errorView.guidance());
        String detail = StringUtils.hasText(errorView.detail()) ? HtmlUtils.htmlEscape(errorView.detail()) : "";
        String statusLine = HtmlUtils.htmlEscape(errorView.status().value() + " " + errorView.status().getReasonPhrase());
        String detailMarkup = StringUtils.hasText(errorView.detail())
                ? """
                    <p class="detail">Details: %s</p>
                  """.formatted(detail)
                : "";

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                  <style>
                    :root {
                      color-scheme: light;
                      --bg: #f5f1e8;
                      --panel: #fffaf2;
                      --line: #d8cdbd;
                      --text: #1f1a17;
                      --muted: #65584d;
                      --accent: #b96c2d;
                    }
                    body {
                      margin: 0;
                      min-height: 100vh;
                      display: grid;
                      place-items: center;
                      background:
                        radial-gradient(circle at top, rgba(185, 108, 45, 0.12), transparent 38%%),
                        linear-gradient(180deg, var(--bg), #efe7d9);
                      color: var(--text);
                      font-family: Georgia, "Times New Roman", serif;
                    }
                    main {
                      width: min(92vw, 640px);
                      padding: 32px;
                      border: 1px solid var(--line);
                      border-radius: 20px;
                      background: var(--panel);
                      box-shadow: 0 16px 48px rgba(63, 45, 29, 0.12);
                    }
                    .eyebrow {
                      margin: 0 0 8px;
                      font-size: 0.8rem;
                      letter-spacing: 0.12em;
                      text-transform: uppercase;
                      color: var(--accent);
                    }
                    h1 {
                      margin: 0 0 12px;
                      font-size: clamp(2rem, 5vw, 3rem);
                      line-height: 1.05;
                    }
                    p {
                      margin: 0 0 14px;
                      font-size: 1.05rem;
                      line-height: 1.6;
                    }
                    .status {
                      margin-top: 22px;
                      color: var(--muted);
                      font-size: 0.95rem;
                    }
                    .detail {
                      padding: 12px 14px;
                      border-radius: 14px;
                      background: rgba(31, 26, 23, 0.05);
                      color: var(--muted);
                      font-size: 0.95rem;
                    }
                    a {
                      display: inline-block;
                      margin-top: 20px;
                      padding: 12px 18px;
                      border-radius: 999px;
                      background: var(--text);
                      color: #fffaf2;
                      text-decoration: none;
                      font-weight: 600;
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <p class="eyebrow">Spotify Flow Error</p>
                    <h1>%s</h1>
                    <p>%s</p>
                    <p>%s</p>
                    %s
                    <p class="status">%s</p>
                    <a href="%s">Return to app</a>
                  </main>
                </body>
                </html>
                """.formatted(title, title, message, guidance, detailMarkup, statusLine, returnHref);
    }

    private static boolean isSpotifyBrowserFlow(HttpServletRequest request) {
        return isConnectRoute(request) || isCallbackRoute(request);
    }

    private static boolean isConnectRoute(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.endsWith("/spotify/connect");
    }

    private static boolean isCallbackRoute(HttpServletRequest request) {
        return "/api/spotify/callback".equals(request.getRequestURI());
    }

    private record BrowserFlowErrorView(
            String title,
            String message,
            String guidance,
            String detail,
            HttpStatus status,
            URI returnUri
    ) {
    }
}
