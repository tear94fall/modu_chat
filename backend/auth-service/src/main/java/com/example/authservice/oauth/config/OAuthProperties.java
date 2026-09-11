package com.example.authservice.oauth.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** modu.oauth.* — 발급자, TTL, 등록 클라이언트, 구글 audience, 서명 키. config-repo 가 내려준다. */
@Data
@ConfigurationProperties(prefix = "modu.oauth")
public class OAuthProperties {
    private String issuer;
    private Duration accessTokenTtl = Duration.ofHours(1);
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private Duration ssoCodeTtl = Duration.ofSeconds(60);
    private Google google = new Google();
    private Rsa rsa = new Rsa();
    private List<Client> clients = new ArrayList<>();

    @Data public static class Google { private List<String> audiences = new ArrayList<>(); }
    @Data public static class Rsa { private String privateKey; private String publicKey; }
    @Data public static class Client {
        private String id;
        private List<String> grants = new ArrayList<>();
        private List<String> scopes = new ArrayList<>();
        /** true 면 이 클라이언트로 로그인한 사용자가 다른 앱에 SSO 코드를 발급해 줄 수 있다. */
        private boolean ssoIssuer;
    }

    public Client client(String id) {
        return clients.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }
}
