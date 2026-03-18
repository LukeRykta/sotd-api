package sotd.song;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Period windows supported by the read endpoints.
 *
 * <p>The current implementation reads from daily rollups and aggregates them into larger windows at
 * query time, so week/month/year support does not require a separate ingestion path yet.
 */
public enum SongPeriodType {
    DAY {
        @Override
        public PeriodWindow resolveWindow(LocalDate anchorDate) {
            return new PeriodWindow(this, anchorDate, anchorDate.plusDays(1));
        }
    },
    WEEK {
        @Override
        public PeriodWindow resolveWindow(LocalDate anchorDate) {
            LocalDate periodStartLocal = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new PeriodWindow(this, periodStartLocal, periodStartLocal.plusWeeks(1));
        }
    },
    MONTH {
        @Override
        public PeriodWindow resolveWindow(LocalDate anchorDate) {
            LocalDate periodStartLocal = anchorDate.withDayOfMonth(1);
            return new PeriodWindow(this, periodStartLocal, periodStartLocal.plusMonths(1));
        }
    },
    YEAR {
        @Override
        public PeriodWindow resolveWindow(LocalDate anchorDate) {
            LocalDate periodStartLocal = anchorDate.withDayOfYear(1);
            return new PeriodWindow(this, periodStartLocal, periodStartLocal.plusYears(1));
        }
    };

    public abstract PeriodWindow resolveWindow(LocalDate anchorDate);

    public record PeriodWindow(
            SongPeriodType periodType,
            LocalDate periodStartLocal,
            LocalDate periodEndExclusive
    ) {
    }
}
