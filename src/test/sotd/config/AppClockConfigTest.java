package sotd.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AppClockConfigTest {

    @Test
    void appClockUsesUtc() {
        AppClockConfig config = new AppClockConfig();

        Clock clock = config.appClock();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
