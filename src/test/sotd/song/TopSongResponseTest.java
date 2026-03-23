package sotd.song;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TopSongResponseTest {

    @Test
    void unlinkedReturnsUserScopedPlaceholderResponse() {
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        TopSongResponse response = TopSongResponse.unlinked(appUserId, SongPeriodType.DAY, LocalDate.parse("2026-03-17"));

        assertThat(response.status()).isEqualTo("unlinked");
        assertThat(response.message()).isEqualTo("No Spotify account is linked for this user.");
        assertThat(response.appUserId()).isEqualTo(appUserId);
        assertThat(response.periodType()).isEqualTo(SongPeriodType.DAY);
        assertThat(response.periodStartLocal()).isEqualTo(LocalDate.parse("2026-03-17"));
        assertThat(response.spotifyUserId()).isNull();
        assertThat(response.trackName()).isNull();
        assertThat(response.imageUrl()).isNull();
    }

    @Test
    void pendingReturnsUserScopedPlaceholderResponse() {
        UUID appUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        TopSongResponse response = TopSongResponse.pending(appUserId, SongPeriodType.MONTH, LocalDate.parse("2026-03-01"));

        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.message()).isEqualTo("No top-song data has been computed yet for the requested period.");
        assertThat(response.appUserId()).isEqualTo(appUserId);
        assertThat(response.periodType()).isEqualTo(SongPeriodType.MONTH);
        assertThat(response.periodStartLocal()).isEqualTo(LocalDate.parse("2026-03-01"));
        assertThat(response.spotifyUserId()).isNull();
        assertThat(response.trackName()).isNull();
        assertThat(response.imageUrl()).isNull();
    }
}
