package sotd.song;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import sotd.song.ListeningStreaksRepository.AccountContext;
import sotd.song.ListeningStreaksRepository.DailyArtistRollup;
import sotd.song.ListeningStreaksRepository.DailyTrackRollup;

/**
 * Computes curated listening-streak insights from existing playback and daily rollup data.
 */
@Service
public class ListeningStreaksService {

    static final String ACTIVE_DAY_TIE_BREAK_RULE = "LONGEST_DAYS_THEN_END_DATE";
    static final String FEATURED_TRACK_TIE_BREAK_RULE =
            "DAYS_THEN_TOTAL_PLAYS_THEN_END_DATE_THEN_LAST_PLAYED_THEN_TRACK_ID";
    static final String FEATURED_ARTIST_TIE_BREAK_RULE =
            "DAYS_THEN_TOTAL_PLAYS_THEN_END_DATE_THEN_ARTIST_ID";
    static final String HEAVIEST_DAY_TIE_BREAK_RULE =
            "TOTAL_PLAYS_THEN_DISTINCT_TRACKS_THEN_DATE";
    static final String REPEAT_OFFENDER_TIE_BREAK_RULE =
            "ACTIVE_DAYS_THEN_TOTAL_PLAYS_THEN_LAST_DAY_THEN_TRACK_ID";

    private final ListeningStreaksRepository listeningStreaksRepository;
    private final Clock clock;

    public ListeningStreaksService(ListeningStreaksRepository listeningStreaksRepository, Clock clock) {
        this.listeningStreaksRepository = listeningStreaksRepository;
        this.clock = clock;
    }

    public ListeningStreaksResponse getListeningStreaks(UUID appUserId, int lookbackDays, int repeatLimit) {
        Instant generatedAt = clock.instant();
        Optional<AccountContext> accountContext = listeningStreaksRepository.findAccountContextByAppUserId(appUserId);
        if (accountContext.isEmpty()) {
            return ListeningStreaksResponse.unlinked(appUserId, lookbackDays, generatedAt);
        }

        LocalDate currentLocalDate = generatedAt.atZone(ZoneId.of(accountContext.get().timezone())).toLocalDate();
        LocalDate windowStartLocal = currentLocalDate.minusDays(lookbackDays - 1L);
        LocalDate windowEndLocalExclusive = currentLocalDate.plusDays(1);

        List<LocalDate> activeListeningDays = listeningStreaksRepository.findActiveListeningDays(
                accountContext.get().accountId(),
                windowStartLocal,
                windowEndLocalExclusive
        );
        if (activeListeningDays.isEmpty()) {
            return ListeningStreaksResponse.noData(
                    accountContext.get(),
                    lookbackDays,
                    windowStartLocal,
                    windowEndLocalExclusive,
                    generatedAt
            );
        }

        List<DailyTrackRollup> dailyTrackRollups = listeningStreaksRepository.findDailyTrackRollups(
                accountContext.get().accountId(),
                windowStartLocal,
                windowEndLocalExclusive
        );
        List<DailyArtistRollup> dailyArtistRollups = listeningStreaksRepository.findDailyArtistRollups(
                accountContext.get().accountId(),
                windowStartLocal,
                windowEndLocalExclusive
        );

        return ListeningStreaksResponse.available(
                accountContext.get(),
                lookbackDays,
                windowStartLocal,
                windowEndLocalExclusive,
                generatedAt,
                buildActiveDayStreak(activeListeningDays, currentLocalDate),
                buildFeaturedTrackStreak(dailyTrackRollups).orElse(null),
                buildFeaturedArtistStreak(dailyArtistRollups).orElse(null),
                buildHeaviestListeningDay(dailyTrackRollups).orElse(null),
                buildRepeatOffenderTracks(dailyTrackRollups, repeatLimit)
        );
    }

    private static ListeningStreaksResponse.ActiveDayStreak buildActiveDayStreak(
            List<LocalDate> activeListeningDays,
            LocalDate currentLocalDate
    ) {
        List<DateStreak> streaks = buildDateStreaks(activeListeningDays);
        DateStreak currentStreak = streaks.get(streaks.size() - 1);
        DateStreak longestStreak = streaks.stream()
                .max(Comparator.comparingInt(DateStreak::days)
                        .thenComparing(DateStreak::endLocal))
                .orElseThrow();

        return new ListeningStreaksResponse.ActiveDayStreak(
                currentStreak.days(),
                currentStreak.startLocal(),
                currentStreak.endLocal(),
                currentStreak.endLocal().equals(currentLocalDate),
                longestStreak.days(),
                longestStreak.startLocal(),
                longestStreak.endLocal(),
                ACTIVE_DAY_TIE_BREAK_RULE
        );
    }

    private static Optional<ListeningStreaksResponse.FeaturedTrackStreak> buildFeaturedTrackStreak(List<DailyTrackRollup> dailyTrackRollups) {
        return buildTrackStreakCandidates(dailyTrackRollups).stream()
                .max(Comparator.comparingInt(TrackStreakCandidate::days)
                        .thenComparingLong(TrackStreakCandidate::totalPlaysAcrossStreak)
                        .thenComparing(TrackStreakCandidate::endLocal)
                        .thenComparing(TrackStreakCandidate::lastPlayedAtUtc)
                        .thenComparing(TrackStreakCandidate::spotifyTrackId, Comparator.reverseOrder()))
                .map(candidate -> new ListeningStreaksResponse.FeaturedTrackStreak(
                        candidate.spotifyTrackId(),
                        candidate.trackName(),
                        candidate.imageUrl(),
                        candidate.days(),
                        candidate.totalPlaysAcrossStreak(),
                        candidate.startLocal(),
                        candidate.endLocal(),
                        FEATURED_TRACK_TIE_BREAK_RULE
                ));
    }

    private static Optional<ListeningStreaksResponse.FeaturedArtistStreak> buildFeaturedArtistStreak(List<DailyArtistRollup> dailyArtistRollups) {
        return buildArtistStreakCandidates(dailyArtistRollups).stream()
                .max(Comparator.comparingInt(ArtistStreakCandidate::days)
                        .thenComparingLong(ArtistStreakCandidate::totalPlaysAcrossStreak)
                        .thenComparing(ArtistStreakCandidate::endLocal)
                        .thenComparing(ArtistStreakCandidate::spotifyArtistId, Comparator.reverseOrder()))
                .map(candidate -> new ListeningStreaksResponse.FeaturedArtistStreak(
                        candidate.spotifyArtistId(),
                        candidate.artistName(),
                        candidate.days(),
                        candidate.totalPlaysAcrossStreak(),
                        candidate.startLocal(),
                        candidate.endLocal(),
                        FEATURED_ARTIST_TIE_BREAK_RULE
                ));
    }

    private static Optional<ListeningStreaksResponse.HeaviestListeningDay> buildHeaviestListeningDay(List<DailyTrackRollup> dailyTrackRollups) {
        Map<LocalDate, DayAggregate> aggregateByDate = new LinkedHashMap<>();
        for (DailyTrackRollup rollup : dailyTrackRollups) {
            aggregateByDate.compute(rollup.dateLocal(), (date, existing) -> existing == null
                    ? new DayAggregate(rollup.playCount(), 1)
                    : new DayAggregate(existing.totalPlays() + rollup.playCount(), existing.distinctTracks() + 1));
        }

        return aggregateByDate.entrySet().stream()
                .max(Comparator.<Map.Entry<LocalDate, DayAggregate>>comparingLong(entry -> entry.getValue().totalPlays())
                        .thenComparingInt(entry -> entry.getValue().distinctTracks())
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new ListeningStreaksResponse.HeaviestListeningDay(
                        entry.getKey(),
                        entry.getValue().totalPlays(),
                        entry.getValue().distinctTracks(),
                        HEAVIEST_DAY_TIE_BREAK_RULE
                ));
    }

    private static List<ListeningStreaksResponse.RepeatOffenderTrack> buildRepeatOffenderTracks(
            List<DailyTrackRollup> dailyTrackRollups,
            int repeatLimit
    ) {
        Map<String, TrackRepeatAggregate> aggregateByTrack = new LinkedHashMap<>();
        for (DailyTrackRollup rollup : dailyTrackRollups) {
            aggregateByTrack.compute(rollup.spotifyTrackId(), (trackId, existing) -> existing == null
                    ? new TrackRepeatAggregate(
                    rollup.spotifyTrackId(),
                    rollup.trackName(),
                    rollup.imageUrl(),
                    1,
                    rollup.playCount(),
                    rollup.dateLocal(),
                    rollup.dateLocal()
            )
                    : new TrackRepeatAggregate(
                    existing.spotifyTrackId(),
                    existing.trackName(),
                    existing.imageUrl(),
                    existing.activeDays() + 1,
                    existing.totalPlays() + rollup.playCount(),
                    existing.firstDayLocal(),
                    rollup.dateLocal()
            ));
        }

        Comparator<TrackRepeatAggregate> repeatOffenderComparator = Comparator
                .comparingInt(TrackRepeatAggregate::activeDays).reversed()
                .thenComparing(Comparator.comparingLong(TrackRepeatAggregate::totalPlays).reversed())
                .thenComparing(Comparator.comparing(TrackRepeatAggregate::lastDayLocal).reversed())
                .thenComparing(TrackRepeatAggregate::spotifyTrackId);

        return aggregateByTrack.values().stream()
                .sorted(repeatOffenderComparator)
                .limit(repeatLimit)
                .map(aggregate -> new ListeningStreaksResponse.RepeatOffenderTrack(
                        aggregate.spotifyTrackId(),
                        aggregate.trackName(),
                        aggregate.imageUrl(),
                        aggregate.activeDays(),
                        aggregate.totalPlays(),
                        aggregate.firstDayLocal(),
                        aggregate.lastDayLocal(),
                        REPEAT_OFFENDER_TIE_BREAK_RULE
                ))
                .toList();
    }

    private static List<TrackStreakCandidate> buildTrackStreakCandidates(List<DailyTrackRollup> dailyTrackRollups) {
        Map<String, List<DailyTrackRollup>> rollupsByTrack = new LinkedHashMap<>();
        for (DailyTrackRollup rollup : dailyTrackRollups) {
            rollupsByTrack.computeIfAbsent(rollup.spotifyTrackId(), ignored -> new ArrayList<>()).add(rollup);
        }

        List<TrackStreakCandidate> candidates = new ArrayList<>();
        for (List<DailyTrackRollup> rollups : rollupsByTrack.values()) {
            TrackStreakAccumulator accumulator = null;
            DailyTrackRollup previousRollup = null;
            for (DailyTrackRollup rollup : rollups) {
                if (accumulator == null || !rollup.dateLocal().equals(previousRollup.dateLocal().plusDays(1))) {
                    if (accumulator != null) {
                        candidates.add(accumulator.finish());
                    }
                    accumulator = TrackStreakAccumulator.from(rollup);
                } else {
                    accumulator = accumulator.add(rollup);
                }
                previousRollup = rollup;
            }
            if (accumulator != null) {
                candidates.add(accumulator.finish());
            }
        }
        return candidates;
    }

    private static List<ArtistStreakCandidate> buildArtistStreakCandidates(List<DailyArtistRollup> dailyArtistRollups) {
        Map<String, List<DailyArtistRollup>> rollupsByArtist = new LinkedHashMap<>();
        for (DailyArtistRollup rollup : dailyArtistRollups) {
            rollupsByArtist.computeIfAbsent(rollup.spotifyArtistId(), ignored -> new ArrayList<>()).add(rollup);
        }

        List<ArtistStreakCandidate> candidates = new ArrayList<>();
        for (List<DailyArtistRollup> rollups : rollupsByArtist.values()) {
            ArtistStreakAccumulator accumulator = null;
            DailyArtistRollup previousRollup = null;
            for (DailyArtistRollup rollup : rollups) {
                if (accumulator == null || !rollup.dateLocal().equals(previousRollup.dateLocal().plusDays(1))) {
                    if (accumulator != null) {
                        candidates.add(accumulator.finish());
                    }
                    accumulator = ArtistStreakAccumulator.from(rollup);
                } else {
                    accumulator = accumulator.add(rollup);
                }
                previousRollup = rollup;
            }
            if (accumulator != null) {
                candidates.add(accumulator.finish());
            }
        }
        return candidates;
    }

    private static List<DateStreak> buildDateStreaks(List<LocalDate> sortedDates) {
        List<DateStreak> streaks = new ArrayList<>();
        LocalDate streakStart = sortedDates.get(0);
        LocalDate previousDate = sortedDates.get(0);
        int streakLength = 1;

        for (int index = 1; index < sortedDates.size(); index++) {
            LocalDate currentDate = sortedDates.get(index);
            if (currentDate.equals(previousDate.plusDays(1))) {
                streakLength++;
            } else {
                streaks.add(new DateStreak(streakStart, previousDate, streakLength));
                streakStart = currentDate;
                streakLength = 1;
            }
            previousDate = currentDate;
        }

        streaks.add(new DateStreak(streakStart, previousDate, streakLength));
        return streaks;
    }

    private record DateStreak(LocalDate startLocal, LocalDate endLocal, int days) {
    }

    private record DayAggregate(long totalPlays, int distinctTracks) {
    }

    private record TrackRepeatAggregate(
            String spotifyTrackId,
            String trackName,
            String imageUrl,
            int activeDays,
            long totalPlays,
            LocalDate firstDayLocal,
            LocalDate lastDayLocal
    ) {
    }

    private record TrackStreakCandidate(
            String spotifyTrackId,
            String trackName,
            String imageUrl,
            int days,
            long totalPlaysAcrossStreak,
            LocalDate startLocal,
            LocalDate endLocal,
            Instant lastPlayedAtUtc
    ) {
    }

    private record ArtistStreakCandidate(
            String spotifyArtistId,
            String artistName,
            int days,
            long totalPlaysAcrossStreak,
            LocalDate startLocal,
            LocalDate endLocal
    ) {
    }

    private record TrackStreakAccumulator(
            String spotifyTrackId,
            String trackName,
            String imageUrl,
            int days,
            long totalPlaysAcrossStreak,
            LocalDate startLocal,
            LocalDate endLocal,
            Instant lastPlayedAtUtc
    ) {
        static TrackStreakAccumulator from(DailyTrackRollup rollup) {
            return new TrackStreakAccumulator(
                    rollup.spotifyTrackId(),
                    rollup.trackName(),
                    rollup.imageUrl(),
                    1,
                    rollup.playCount(),
                    rollup.dateLocal(),
                    rollup.dateLocal(),
                    rollup.lastPlayedAtUtc()
            );
        }

        TrackStreakAccumulator add(DailyTrackRollup rollup) {
            return new TrackStreakAccumulator(
                    spotifyTrackId,
                    trackName,
                    imageUrl,
                    days + 1,
                    totalPlaysAcrossStreak + rollup.playCount(),
                    startLocal,
                    rollup.dateLocal(),
                    lastPlayedAtUtc.isAfter(rollup.lastPlayedAtUtc()) ? lastPlayedAtUtc : rollup.lastPlayedAtUtc()
            );
        }

        TrackStreakCandidate finish() {
            return new TrackStreakCandidate(
                    spotifyTrackId,
                    trackName,
                    imageUrl,
                    days,
                    totalPlaysAcrossStreak,
                    startLocal,
                    endLocal,
                    lastPlayedAtUtc
            );
        }
    }

    private record ArtistStreakAccumulator(
            String spotifyArtistId,
            String artistName,
            int days,
            long totalPlaysAcrossStreak,
            LocalDate startLocal,
            LocalDate endLocal
    ) {
        static ArtistStreakAccumulator from(DailyArtistRollup rollup) {
            return new ArtistStreakAccumulator(
                    rollup.spotifyArtistId(),
                    rollup.artistName(),
                    1,
                    rollup.playCount(),
                    rollup.dateLocal(),
                    rollup.dateLocal()
            );
        }

        ArtistStreakAccumulator add(DailyArtistRollup rollup) {
            return new ArtistStreakAccumulator(
                    spotifyArtistId,
                    artistName,
                    days + 1,
                    totalPlaysAcrossStreak + rollup.playCount(),
                    startLocal,
                    rollup.dateLocal()
            );
        }

        ArtistStreakCandidate finish() {
            return new ArtistStreakCandidate(
                    spotifyArtistId,
                    artistName,
                    days,
                    totalPlaysAcrossStreak,
                    startLocal,
                    endLocal
            );
        }
    }
}
