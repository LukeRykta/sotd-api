package sotd.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sotd.config.OpenApiConfig;
import sotd.song.ListeningStreaksResponse;
import sotd.song.ListeningStreaksService;

@Validated
@RestController
@RequestMapping("/api/users/{appUserId}")
@Tag(name = "listening-streaks")
public class ListeningStreaksController {

    private final ListeningStreaksService listeningStreaksService;

    public ListeningStreaksController(ListeningStreaksService listeningStreaksService) {
        this.listeningStreaksService = listeningStreaksService;
    }

    @GetMapping("/listening-streaks")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get curated listening streak insights for one user",
            description = "Returns DB-backed listening streak and recurrence insights computed from recent playback and daily rollups.",
            security = @SecurityRequirement(name = OpenApiConfig.UPSTREAM_HEADER_AUTH_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listening streak state returned."),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid upstream auth token.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Upstream token does not match the requested UUID.", content = @Content)
    })
    public ListeningStreaksResponse getListeningStreaks(
            @Parameter(description = "Stable upstream application user UUID.", required = true)
            @PathVariable UUID appUserId,
            @Parameter(
                    description = "Number of local days to inspect, counting back from the user's current local day.",
                    in = ParameterIn.QUERY
            )
            @RequestParam(defaultValue = "90")
            @Min(1) @Max(365) int lookbackDays,
            @Parameter(
                    description = "Maximum number of recurring tracks to return.",
                    in = ParameterIn.QUERY
            )
            @RequestParam(defaultValue = "5")
            @Min(1) @Max(10) int repeatLimit
    ) {
        return listeningStreaksService.getListeningStreaks(appUserId, lookbackDays, repeatLimit);
    }
}
