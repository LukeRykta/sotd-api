package sotd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import sotd.crypto.CryptoProperties;
import sotd.spotify.SpotifyProperties;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({SpotifyProperties.class, CryptoProperties.class})
public class SotdApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SotdApiApplication.class, args);
    }
}
