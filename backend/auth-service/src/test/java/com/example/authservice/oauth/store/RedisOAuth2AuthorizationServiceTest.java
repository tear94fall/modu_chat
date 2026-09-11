package com.example.authservice.oauth.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@SpringBootTest
class RedisOAuth2AuthorizationServiceTest {

    @Autowired OAuth2AuthorizationService service;
    @Autowired RegisteredClientRepository clients;

    private OAuth2Authorization sample(String id) {
        RegisteredClient chat = clients.findByClientId("modu-chat");
        Instant now = Instant.now();
        var principal = new UsernamePasswordAuthenticationToken("user-1", null, Set.of(new SimpleGrantedAuthority("ROLE_USER")));
        return OAuth2Authorization.withRegisteredClient(chat)
                .id(id).principalName("user-1")
                .authorizationGrantType(new AuthorizationGrantType("urn:modu:params:oauth:grant-type:google_id_token"))
                .authorizedScopes(Set.of("openid", "chat"))
                .attribute(Principal.class.getName(), principal)
                .token(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-" + id, now, now.plusSeconds(3600), Set.of("openid", "chat")),
                        md -> md.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, Map.of("sub", "user-1", "roles", List.of("ROLE_USER"))))
                .refreshToken(new OAuth2RefreshToken("refresh-" + id, now, now.plusSeconds(7 * 24 * 3600)))
                .build();
    }

    @Test
    void 저장한_인가를_id_와_각_토큰으로_찾고_삭제하면_전부_사라진다() {
        service.save(sample("a1"));

        OAuth2Authorization byId = service.findById("a1");
        assertThat(byId).isNotNull();
        assertThat(byId.getPrincipalName()).isEqualTo("user-1");
        assertThat(byId.getAuthorizedScopes()).containsExactlyInAnyOrder("openid", "chat");
        assertThat(byId.getAccessToken().getClaims()).containsEntry("sub", "user-1");
        assertThat((Object) byId.getAttribute(Principal.class.getName())).isInstanceOf(UsernamePasswordAuthenticationToken.class);

        assertThat(service.findByToken("access-a1", OAuth2TokenType.ACCESS_TOKEN).getId()).isEqualTo("a1");
        assertThat(service.findByToken("refresh-a1", OAuth2TokenType.REFRESH_TOKEN).getId()).isEqualTo("a1");
        assertThat(service.findByToken("refresh-a1", null).getId()).isEqualTo("a1");
        assertThat(service.findByToken("nope", null)).isNull();

        service.remove(byId);
        assertThat(service.findById("a1")).isNull();
        assertThat(service.findByToken("access-a1", OAuth2TokenType.ACCESS_TOKEN)).isNull();
        assertThat(service.findByToken("refresh-a1", OAuth2TokenType.REFRESH_TOKEN)).isNull();
    }

    @Test
    void 같은_id_로_다시_저장하면_옛_토큰_인덱스는_정리된다() {
        service.save(sample("a2"));
        OAuth2Authorization rotated = OAuth2Authorization.from(service.findById("a2"))
                .refreshToken(new OAuth2RefreshToken("refresh-a2-new", Instant.now(), Instant.now().plusSeconds(600)))
                .build();
        service.save(rotated);

        assertThat(service.findByToken("refresh-a2-new", OAuth2TokenType.REFRESH_TOKEN)).isNotNull();
        assertThat(service.findByToken("refresh-a2", OAuth2TokenType.REFRESH_TOKEN)).isNull();
    }
}
