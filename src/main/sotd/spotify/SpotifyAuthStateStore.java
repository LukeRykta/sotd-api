package sotd.spotify;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
/**
 * In-memory storage for OAuth state values during the Spotify connect flow.
 *
 * <p>This is intentionally simple for the current single-instance POC. It must move to a shared store
 * before multi-instance deployment.
 */
public class SpotifyAuthStateStore {

    private static final int STATE_BYTES = 24;

    private final Clock clock;
    private final RandomStateGenerator randomStateGenerator;
    private final Map<String, IssuedState> issuedStates = new ConcurrentHashMap<>();

    @Autowired
    public SpotifyAuthStateStore(Clock clock) {
        this(clock, new RandomStateGenerator());
    }

    SpotifyAuthStateStore(Clock clock, RandomStateGenerator randomStateGenerator) {
        this.clock = clock;
        this.randomStateGenerator = randomStateGenerator;
    }

    /**
     * Issues a one-time state token that expires at the supplied timestamp.
     */
    public String issueState(UUID appUserId, Instant expiresAt) {
        evictExpired(clock.instant());
        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomStateGenerator.generate(STATE_BYTES));
        issuedStates.put(state, new IssuedState(appUserId, expiresAt));
        return state;
    }

    /**
     * Consumes a state token once and returns whether it was valid and unexpired.
     */
    public Optional<UUID> consume(String state) {
        if (state == null) {
            return Optional.empty();
        }

        Instant now = clock.instant();
        evictExpired(now);
        IssuedState issuedState = issuedStates.remove(state);
        return Optional.ofNullable(issuedState)
                .filter(savedState -> savedState.expiresAt().isAfter(now))
                .map(IssuedState::appUserId);
    }

    private void evictExpired(Instant now) {
        issuedStates.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    record IssuedState(
            UUID appUserId,
            Instant expiresAt
    ) {
    }
}
