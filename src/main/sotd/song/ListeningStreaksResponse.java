package sotd.song;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Listening streak insights for one application user.")
public record ListeningStreaksResponse(
        @Schema(description = "High-level response state.", allowableValues = {"ready", "no-data", "unlinked"}, example = "ready")
        String status,
        @Schema(description = "Human-readable explanation of the current state.", example = "Listening streak insights computed successfully.")
        String message,
        @Schema(description = "Stable upstream application user UUID.", example = "11111111-1111-1111-1111-111111111111")
        UUID appUserId,
        @Schema(description = "Linked Spotify user id when available.", example = "lukerykta", nullable = true)
        String spotifyUserId,
        @Schema(description = "Spotify display name when available.", example = "lukerykta", nullable = true)
        String displayName,
        @Schema(description = "IANA timezone used for local-day calculations when available.", example = "America/Chicago", nullable = true)
        String timezone,
        @Schema(description = "Lookback window in local days.", example = "90")
        Integer lookbackDays,
        @Schema(description = "Inclusive local start of the lookback window.", example = "2025-12-20", nullable = true)
        LocalDate windowStartLocal,
        @Schema(description = "Exclusive local end of the lookback window.", example = "2026-03-20", nullable = true)
        LocalDate windowEndLocalExclusive,
        @Schema(description = "When the insight response was generated.", example = "2026-03-19T00:16:32.736306900Z")
        Instant generatedAt,
        @Schema(description = "Current and longest active listening-day streaks.")
        ActiveDayStreak activeDayStreak,
        @Schema(description = "Strongest repeated track streak in the window.")
        FeaturedTrackStreak featuredTrackStreak,
        @Schema(description = "Strongest repeated artist streak in the window.")
        FeaturedArtistStreak featuredArtistStreak,
        @Schema(description = "The single heaviest listening day in the window.")
        HeaviestListeningDay heaviestListeningDay,
        @Schema(description = "Most frequently recurring tracks across separate days in the window.")
        List<RepeatOffenderTrack> repeatOffenderTracks
) {

    public static ListeningStreaksResponse unlinked(UUID appUserId, int lookbackDays, Instant generatedAt) {
        return new ListeningStreaksResponse(
                "unlinked",
                "No Spotify account is linked for this user.",
                appUserId,
                null,
                null,
                null,
                lookbackDays,
                null,
                null,
                generatedAt,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    public static ListeningStreaksResponse noData(
            ListeningStreaksRepository.AccountContext accountContext,
            int lookbackDays,
            LocalDate windowStartLocal,
            LocalDate windowEndLocalExclusive,
            Instant generatedAt
    ) {
        return new ListeningStreaksResponse(
                "no-data",
                "No listening data was found in the requested window.",
                accountContext.appUserId(),
                accountContext.spotifyUserId(),
                accountContext.displayName(),
                accountContext.timezone(),
                lookbackDays,
                windowStartLocal,
                windowEndLocalExclusive,
                generatedAt,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    public static ListeningStreaksResponse available(
            ListeningStreaksRepository.AccountContext accountContext,
            int lookbackDays,
            LocalDate windowStartLocal,
            LocalDate windowEndLocalExclusive,
            Instant generatedAt,
            ActiveDayStreak activeDayStreak,
            FeaturedTrackStreak featuredTrackStreak,
            FeaturedArtistStreak featuredArtistStreak,
            HeaviestListeningDay heaviestListeningDay,
            List<RepeatOffenderTrack> repeatOffenderTracks
    ) {
        return new ListeningStreaksResponse(
                "ready",
                "Listening streak insights computed successfully.",
                accountContext.appUserId(),
                accountContext.spotifyUserId(),
                accountContext.displayName(),
                accountContext.timezone(),
                lookbackDays,
                windowStartLocal,
                windowEndLocalExclusive,
                generatedAt,
                activeDayStreak,
                featuredTrackStreak,
                featuredArtistStreak,
                heaviestListeningDay,
                List.copyOf(repeatOffenderTracks)
        );
    }

    @Schema(description = "Current and longest active listening-day streaks.")
    public record ActiveDayStreak(
            @Schema(description = "Current consecutive active-day streak ending on the latest active local day.", example = "4")
            Integer currentDays,
            @Schema(description = "Local start of the current streak.", example = "2026-03-15")
            LocalDate currentStartLocal,
            @Schema(description = "Local end of the current streak.", example = "2026-03-18")
            LocalDate currentEndLocal,
            @Schema(description = "Whether the current streak includes the user's current local day.", example = "true")
            boolean includesToday,
            @Schema(description = "Longest active-day streak found in the window.", example = "14")
            Integer longestDays,
            @Schema(description = "Local start of the longest streak.", example = "2026-02-10")
            LocalDate longestStartLocal,
            @Schema(description = "Local end of the longest streak.", example = "2026-02-23")
            LocalDate longestEndLocal,
            @Schema(description = "Deterministic tie-break rule for choosing the longest streak.", example = "LONGEST_DAYS_THEN_END_DATE")
            String tieBreakRule
    ) {
    }

    @Schema(description = "Strongest repeated track streak within the requested window.")
    public record FeaturedTrackStreak(
            @Schema(description = "Spotify track id.", example = "5u6y4u5EgDv0peILf60H5t")
            String spotifyTrackId,
            @Schema(description = "Track name.", example = "Oye Como Va")
            String trackName,
            @Schema(description = "Number of consecutive local days in the streak.", example = "3")
            Integer days,
            @Schema(description = "Total plays for the track across the streak.", example = "8")
            Long totalPlaysAcrossStreak,
            @Schema(description = "Local start of the streak.", example = "2026-03-16")
            LocalDate startLocal,
            @Schema(description = "Local end of the streak.", example = "2026-03-18")
            LocalDate endLocal,
            @Schema(description = "Deterministic tie-break rule used for track streak ranking.", example = "DAYS_THEN_TOTAL_PLAYS_THEN_END_DATE_THEN_LAST_PLAYED_THEN_TRACK_ID")
            String tieBreakRule
    ) {
    }

    @Schema(description = "Strongest repeated artist streak within the requested window.")
    public record FeaturedArtistStreak(
            @Schema(description = "Spotify artist id.", example = "5vfEaoOBcK0Lzr07WN8KaK")
            String spotifyArtistId,
            @Schema(description = "Artist name.", example = "Darius")
            String artistName,
            @Schema(description = "Number of consecutive local days in the streak.", example = "5")
            Integer days,
            @Schema(description = "Total plays credited to the artist across the streak.", example = "17")
            Long totalPlaysAcrossStreak,
            @Schema(description = "Local start of the streak.", example = "2026-03-14")
            LocalDate startLocal,
            @Schema(description = "Local end of the streak.", example = "2026-03-18")
            LocalDate endLocal,
            @Schema(description = "Deterministic tie-break rule used for artist streak ranking.", example = "DAYS_THEN_TOTAL_PLAYS_THEN_END_DATE_THEN_ARTIST_ID")
            String tieBreakRule
    ) {
    }

    @Schema(description = "Single local day with the highest listening volume.")
    public record HeaviestListeningDay(
            @Schema(description = "Local date of the heaviest listening day.", example = "2026-03-18")
            LocalDate dateLocal,
            @Schema(description = "Total plays on that local day.", example = "226")
            Long totalPlays,
            @Schema(description = "Distinct tracks listened to on that day.", example = "224")
            Integer distinctTracks,
            @Schema(description = "Deterministic tie-break rule used for day ranking.", example = "TOTAL_PLAYS_THEN_DISTINCT_TRACKS_THEN_DATE")
            String tieBreakRule
    ) {
    }

    @Schema(description = "Track recurring on the highest number of separate listening days in the window.")
    public record RepeatOffenderTrack(
            @Schema(description = "Spotify track id.", example = "0EmZ1tH478wQbhAXiS4BoM")
            String spotifyTrackId,
            @Schema(description = "Track name.", example = "Rocket Scientist (feat. Eve)")
            String trackName,
            @Schema(description = "Number of separate active days for the track.", example = "6")
            Integer activeDays,
            @Schema(description = "Total plays across the window.", example = "11")
            Long totalPlays,
            @Schema(description = "First local day the track appeared in the window.", example = "2026-03-01")
            LocalDate firstDayLocal,
            @Schema(description = "Last local day the track appeared in the window.", example = "2026-03-18")
            LocalDate lastDayLocal,
            @Schema(description = "Deterministic tie-break rule used for repeat-offender ranking.", example = "ACTIVE_DAYS_THEN_TOTAL_PLAYS_THEN_LAST_DAY_THEN_TRACK_ID")
            String tieBreakRule
    ) {
    }
}
