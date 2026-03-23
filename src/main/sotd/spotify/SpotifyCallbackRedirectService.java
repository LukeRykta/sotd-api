package sotd.spotify;

import java.net.URI;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds the frontend redirect targets used after the Spotify callback completes.
 */
@Service
public class SpotifyCallbackRedirectService {

    private static final String STATUS_PARAM = "spotifyAuthStatus";
    private static final String ERROR_CODE_PARAM = "spotifyAuthErrorCode";
    private static final String STAGE_PARAM = "spotifyAuthStage";
    private static final String APP_USER_ID_PARAM = "appUserId";

    private final SpotifyProperties spotifyProperties;

    public SpotifyCallbackRedirectService(SpotifyProperties spotifyProperties) {
        this.spotifyProperties = spotifyProperties;
    }

    public URI buildSuccessRedirect(SpotifyConnectionResponse response) {
        return baseBuilder()
                .replaceQueryParam(STATUS_PARAM, response.status())
                .replaceQueryParam(APP_USER_ID_PARAM, response.appUserId())
                .replaceQueryParam(ERROR_CODE_PARAM)
                .replaceQueryParam(STAGE_PARAM)
                .build(true)
                .toUri();
    }

    public URI buildFailureRedirect(SpotifyCallbackException exception) {
        UriComponentsBuilder builder = baseBuilder()
                .replaceQueryParam(STATUS_PARAM, "error")
                .replaceQueryParam(ERROR_CODE_PARAM, exception.getErrorCode())
                .replaceQueryParam(STAGE_PARAM, exception.getStage().name());

        UUID appUserId = exception.getAppUserId();
        if (appUserId != null) {
            builder.replaceQueryParam(APP_USER_ID_PARAM, appUserId);
        }
        else {
            builder.replaceQueryParam(APP_USER_ID_PARAM);
        }

        return builder.build(true).toUri();
    }

    private UriComponentsBuilder baseBuilder() {
        return UriComponentsBuilder.fromUri(spotifyProperties.getCallbackFrontendRedirectUri());
    }
}
