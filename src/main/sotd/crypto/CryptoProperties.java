package sotd.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sotd.crypto")
public class CryptoProperties {

    private String base64Key = "";

    public String getBase64Key() {
        return base64Key;
    }

    public void setBase64Key(String base64Key) {
        this.base64Key = base64Key;
    }
}
