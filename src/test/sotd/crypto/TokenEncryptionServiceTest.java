package sotd.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TokenEncryptionServiceTest {

    @Test
    void encryptAndDecryptRoundTrip() {
        CryptoProperties properties = new CryptoProperties();
        properties.setBase64Key(Base64.getEncoder().encodeToString(new byte[32]));
        TokenEncryptionService service = new TokenEncryptionService(properties, new SecureRandom());

        byte[] encrypted = service.encrypt("refresh-token");

        assertThat(encrypted).isNotEmpty();
        assertThat(service.decrypt(encrypted)).isEqualTo("refresh-token");
    }

    @Test
    void encryptRejectsMissingPlaintext() {
        CryptoProperties properties = new CryptoProperties();
        properties.setBase64Key(Base64.getEncoder().encodeToString(new byte[32]));
        TokenEncryptionService service = new TokenEncryptionService(properties, new SecureRandom());

        assertThatThrownBy(() -> service.encrypt(""))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Refresh token is missing");
    }

    @Test
    void encryptRejectsMissingKey() {
        TokenEncryptionService service = new TokenEncryptionService(new CryptoProperties(), new SecureRandom());

        assertThatThrownBy(() -> service.encrypt("refresh-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("SOTD_CRYPTO_BASE64_KEY");
    }
}
