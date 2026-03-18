package sotd.song;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SongOfDayResponseTest {

    @Test
    void unlinkedReturnsUserScopedPlaceholderResponse() {
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        SongOfDayResponse response = SongOfDayResponse.unlinked(appUserId);

        assertThat(response.status()).isEqualTo("unlinked");
        assertThat(response.message()).isEqualTo("No Spotify account is linked for this user.");
        assertThat(response.appUserId()).isEqualTo(appUserId);
        assertThat(response.spotifyUserId()).isNull();
        assertThat(response.trackName()).isNull();
    }

    @Test
    void pendingReturnsUserScopedPlaceholderResponse() {
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        SongOfDayResponse response = SongOfDayResponse.pending(appUserId);

        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.message()).isEqualTo("No song-of-the-day data has been computed yet.");
        assertThat(response.appUserId()).isEqualTo(appUserId);
        assertThat(response.spotifyUserId()).isNull();
        assertThat(response.trackName()).isNull();
    }
}
