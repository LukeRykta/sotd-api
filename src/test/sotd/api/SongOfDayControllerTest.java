package sotd.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import sotd.song.SongOfDayResponse;
import sotd.song.SongOfDayService;

class SongOfDayControllerTest {

    @Test
    void getSongOfTheDayDelegatesToService() {
        SongOfDayService songOfDayService = mock(SongOfDayService.class);
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        SongOfDayResponse expected = new SongOfDayResponse(
                "ready",
                "Result available",
                appUserId,
                "lukerykta",
                "Luke",
                null,
                "track-1",
                "Track Name",
                4
        );
        when(songOfDayService.getCurrentSongOfDay(appUserId)).thenReturn(expected);

        SongOfDayController controller = new SongOfDayController(songOfDayService);

        SongOfDayResponse actual = controller.getSongOfTheDay(appUserId);

        assertThat(actual).isSameAs(expected);
        verify(songOfDayService).getCurrentSongOfDay(appUserId);
    }
}
