package sotd.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class SchedulingConfigTest {

    @Test
    void taskSchedulerUsesExpectedPoolAndThreadPrefix() throws Exception {
        SchedulingConfig config = new SchedulingConfig();
        ThreadPoolTaskScheduler scheduler = config.taskScheduler();
        CompletableFuture<String> threadName = new CompletableFuture<>();

        scheduler.initialize();
        try {
            scheduler.schedule(() -> threadName.complete(Thread.currentThread().getName()), Instant.now());

            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(4);
            assertThat(threadName.get(5, TimeUnit.SECONDS)).startsWith("sotd-");
        }
        finally {
            scheduler.shutdown();
        }
    }
}
