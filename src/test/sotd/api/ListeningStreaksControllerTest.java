package sotd.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sotd.song.ListeningStreaksResponse;
import sotd.song.ListeningStreaksService;

class ListeningStreaksControllerTest {

    @Test
    void getListeningStreaksDelegatesToService() {
        ListeningStreaksService service = mock(ListeningStreaksService.class);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ListeningStreaksResponse expected = new ListeningStreaksResponse(
                "ready",
                "Listening streak insights computed successfully.",
                appUserId,
                "lukerykta",
                "Luke",
                "America/Chicago",
                90,
                LocalDate.parse("2025-12-19"),
                LocalDate.parse("2026-03-19"),
                Instant.parse("2026-03-18T17:00:00Z"),
                null,
                null,
                null,
                null,
                List.of()
        );
        when(service.getListeningStreaks(appUserId, 90, 5)).thenReturn(expected);

        ListeningStreaksController controller = new ListeningStreaksController(service);

        ListeningStreaksResponse actual = controller.getListeningStreaks(appUserId, 90, 5);

        assertThat(actual).isSameAs(expected);
        verify(service).getListeningStreaks(appUserId, 90, 5);
    }
}
