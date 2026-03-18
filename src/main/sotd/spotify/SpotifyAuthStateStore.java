package sotd.spotify;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpotifyAuthStateStore {

    private static final int STATE_BYTES = 24;

    private final Clock clock;
    private final RandomStateGenerator randomStateGenerator;
    private final Map<String, Instant> stateExpirations = new ConcurrentHashMap<>();

    @Autowired
    public SpotifyAuthStateStore(Clock clock) {
        this(clock, new RandomStateGenerator());
    }

    SpotifyAuthStateStore(Clock clock, RandomStateGenerator randomStateGenerator) {
        this.clock = clock;
        this.randomStateGenerator = randomStateGenerator;
    }

    public String issueState(Instant expiresAt) {
        evictExpired(clock.instant());
        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomStateGenerator.generate(STATE_BYTES));
        stateExpirations.put(state, expiresAt);
        return state;
    }

    public boolean consume(String state) {
        if (state == null) {
            return false;
        }

        Instant now = clock.instant();
        evictExpired(now);
        Instant expiresAt = stateExpirations.remove(state);
        return Optional.ofNullable(expiresAt)
                .map(expiry -> expiry.isAfter(now))
                .orElse(false);
    }

    private void evictExpired(Instant now) {
        stateExpirations.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
