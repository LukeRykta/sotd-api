package sotd.spotify;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SpotifyAuthStateStoreTest {

    @Test
    void consumeReturnsTrueForFreshStateAndFalseAfterReuse() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-17T20:00:00Z"), ZoneOffset.UTC);
        SpotifyAuthStateStore store = new SpotifyAuthStateStore(clock, new FixedStateGenerator());

        String state = store.issueState(Instant.parse("2026-03-17T20:10:00Z"));

        assertThat(state).isNotBlank();
        assertThat(store.consume(state)).isTrue();
        assertThat(store.consume(state)).isFalse();
    }

    @Test
    void consumeReturnsFalseForExpiredState() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-17T20:00:00Z"), ZoneOffset.UTC);
        SpotifyAuthStateStore store = new SpotifyAuthStateStore(clock, new FixedStateGenerator());

        String state = store.issueState(Instant.parse("2026-03-17T19:59:00Z"));

        assertThat(store.consume(state)).isFalse();
    }

    private static class FixedStateGenerator extends RandomStateGenerator {
        @Override
        byte[] generate(int length) {
            return new byte[length];
        }
    }
}
