package sotd.spotify;

import java.security.SecureRandom;

class RandomStateGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    byte[] generate(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
}
