package sotd.song;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SongOfDayServiceTest {

    @Test
    void getCurrentSongOfDayReturnsUnavailablePlaceholder() {
        SongOfDayService service = new SongOfDayService();

        SongOfDayResponse response = service.getCurrentSongOfDay();

        assertThat(response).isEqualTo(SongOfDayResponse.unavailable());
    }
}
