package sotd.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import sotd.song.SongOfDayResponse;
import sotd.song.SongOfDayService;

class SongOfDayControllerTest {

    @Test
    void getSongOfTheDayDelegatesToService() {
        SongOfDayService songOfDayService = mock(SongOfDayService.class);
        SongOfDayResponse expected = new SongOfDayResponse("ready", "Result available");
        when(songOfDayService.getCurrentSongOfDay()).thenReturn(expected);

        SongOfDayController controller = new SongOfDayController(songOfDayService);

        SongOfDayResponse actual = controller.getSongOfTheDay();

        assertThat(actual).isSameAs(expected);
        verify(songOfDayService).getCurrentSongOfDay();
    }
}
