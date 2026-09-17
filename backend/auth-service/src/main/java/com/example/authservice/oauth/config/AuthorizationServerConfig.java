package com.example.authservice.oauth.config;

import com.example.authservice.admin.AdminLoginService;
import com.example.authservice.member.client.MemberFeignClient;
import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.oauth.google.GoogleIdTokenVerifierService;
import com.example.authservice.oauth.grant.GrantSupport;
import com.example.authservice.oauth.grant.PublicClientAuthenticationConverter;
import com.example.authservice.oauth.grant.PublicClientAuthenticationProvider;
import com.example.authservice.oauth.grant.admin.AdminPasswordGrantConverter;
import com.example.authservice.oauth.grant.admin.AdminPasswordGrantProvider;
import com.example.authservice.oauth.grant.google.GoogleIdTokenGrantConverter;
import com.example.authservice.oauth.grant.google.GoogleIdTokenGrantProvider;
import com.example.authservice.oauth.grant.sso.SsoCodeGrantConverter;
import com.example.authservice.oauth.grant.sso.SsoCodeGrantProvider;
import com.example.authservice.oauth.sso.SsoCodeStore;
import com.example.authservice.oauth.store.RedisOAuth2AuthorizationService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

/**
 * auth-service 를 OAuth 2.0 / OIDC 인증 서버로 만든다. 브라우저 인가 코드 흐름은 쓰지 않고
 * 커스텀 grant 세 개(구글 ID 토큰, 앱 간 SSO 코드, 관리자 비밀번호)로 토큰을 발급한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    public static final AuthorizationGrantType GOOGLE_ID_TOKEN = new AuthorizationGrantType("urn:modu:params:oauth:grant-type:google_id_token");
    public static final AuthorizationGrantType SSO_CODE = new AuthorizationGrantType("urn:modu:params:oauth:grant-type:sso_code");
    public static final AuthorizationGrantType ADMIN_PASSWORD = new AuthorizationGrantType("urn:modu:params:oauth:grant-type:admin_password");

    private final OAuthProperties props;

    /** 설정의 grants 이름을 AuthorizationGrantType 으로. 모르는 이름은 기동 실패. */
    static AuthorizationGrantType grantOf(String name) {
        return switch (name) {
            case "google_id_token" -> GOOGLE_ID_TOKEN;
            case "sso_code" -> SSO_CODE;
            case "admin_password" -> ADMIN_PASSWORD;
            case "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN;
            default -> throw new IllegalArgumentException("지원하지 않는 grant: " + name);
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerChain(HttpSecurity http, RegisteredClientRepository clients,
                                                        OAuth2AuthorizationService authorizationService,
                                                        OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
                                                        GoogleIdTokenVerifierService verifier, MemberFeignClient members,
                                                        SsoCodeStore ssoCodeStore, AdminLoginService adminLoginService) throws Exception {
        OAuth2AuthorizationServerConfigurer as = OAuth2AuthorizationServerConfigurer.authorizationServer();
        GrantSupport support = new GrantSupport(tokenGenerator, authorizationService);

        http.securityMatcher(as.getEndpointsMatcher())
                .with(as, c -> c
                        .clientAuthentication(ca -> ca
                                .authenticationConverter(new PublicClientAuthenticationConverter())
                                .authenticationProvider(new PublicClientAuthenticationProvider(clients)))
                        .tokenEndpoint(t -> t
                                .accessTokenRequestConverters(list -> {
                                    list.add(new GoogleIdTokenGrantConverter());
                                    list.add(new SsoCodeGrantConverter());
                                    list.add(new AdminPasswordGrantConverter());
                                })
                                .authenticationProviders(list -> {
                                    list.add(new GoogleIdTokenGrantProvider(verifier, members, support));
                                    list.add(new SsoCodeGrantProvider(ssoCodeStore, members, support));
                                    list.add(new AdminPasswordGrantProvider(adminLoginService, support));
                                }))
                        .oidc(oidc -> oidc
                                // 메타데이터에 커스텀 grant 를 알린다. 라이브러리는 표준 grant 만 나열한다.
                                .providerConfigurationEndpoint(p -> p.providerConfigurationCustomizer(b -> b
                                        .grantType(GOOGLE_ID_TOKEN.getValue())
                                        .grantType(SSO_CODE.getValue())
                                        .grantType(ADMIN_PASSWORD.getValue())))
                                .userInfoEndpoint(u -> u.userInfoMapper(ctx -> {
                            String sub = ctx.getAuthorization().getPrincipalName();
                            MemberDto m = members.getMember(sub);
                            Map<String, Object> claims = new HashMap<>();
                            claims.put("sub", sub);
                            claims.put("name", m == null || m.getUsername() == null ? "" : m.getUsername());
                            claims.put("email", m == null || m.getEmail() == null ? "" : m.getEmail());
                            claims.put("picture", m == null || m.getProfileImage() == null ? "" : m.getProfileImage());
                            return new OidcUserInfo(claims);
                        }))))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(as.getEndpointsMatcher()))
                // Bearer 토큰은 /userinfo 에서만 읽는다. 앱이 만료된 액세스 토큰을 기본 헤더로 달고 /oauth2/token 을
                // 불러도(재로그인·재발급) 리소스 서버가 먼저 401 을 내지 않게 한다.
                .oauth2ResourceServer(rs -> rs
                        .bearerTokenResolver(request -> request.getRequestURI().endsWith("/userinfo")
                                ? new DefaultBearerTokenResolver().resolve(request) : null)
                        .jwt(Customizer.withDefaults()));
        return http.build();
    }

    /** 나머지 경로(내부 API, sso-code, actuator): 게이트웨이·내부 필터가 지키므로 permitAll, 세션 없음. */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        List<RegisteredClient> clients = props.getClients().stream().map(c -> {
            RegisteredClient.Builder b = RegisteredClient.withId(UUID.nameUUIDFromBytes(c.getId().getBytes()).toString())
                    .clientId(c.getId())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).requireProofKey(true).build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(props.getAccessTokenTtl())
                            .refreshTokenTimeToLive(props.getRefreshTokenTtl())
                            .reuseRefreshTokens(false)
                            .build());
            c.getGrants().forEach(g -> b.authorizationGrantType(grantOf(g)));
            c.getScopes().forEach(b::scope);
            return b.build();
        }).toList();
        return new InMemoryRegisteredClientRepository(clients);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKey priv = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(pem(props.getRsa().getPrivateKey(), "PRIVATE KEY")));
        RSAPublicKey pub = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(pem(props.getRsa().getPublicKey(), "PUBLIC KEY")));
        String kid = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pub.getEncoded())).substring(0, 16);
        RSAKey key = new RSAKey.Builder(pub).privateKey(priv).keyID(kid).build();
        return new ImmutableJWKSet<>(new JWKSet(key));
    }

    private static byte[] pem(String pemText, String label) {
        if (pemText == null) {
            throw new IllegalStateException("modu.oauth.rsa." + (label.startsWith("PRIVATE") ? "private-key" : "public-key") + " 가 없습니다.");
        }
        String body = pemText.replace("-----BEGIN " + label + "-----", "").replace("-----END " + label + "-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer(props.getIssuer()).build();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(StringRedisTemplate redis, RegisteredClientRepository clients) {
        return new RedisOAuth2AuthorizationService(redis, clients);
    }

    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(JWKSource<SecurityContext> jwkSource,
                                                                    OAuth2TokenCustomizer<JwtEncodingContext> customizer) {
        JwtGenerator jwt = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
        jwt.setJwtCustomizer(customizer);
        return new DelegatingOAuth2TokenGenerator(jwt, new OAuth2AccessTokenGenerator(), new OAuth2RefreshTokenGenerator());
    }

    /** 액세스 토큰에 roles 를 넣는다. 게이트웨이가 ROLE_* 로 라우트를 지킨다. */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> rolesCustomizer() {
        return ctx -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(ctx.getTokenType())) {
                List<String> roles = ctx.getPrincipal().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
                ctx.getClaims().claim("roles", roles);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
