package com.example.authservice.oauth.config

import com.example.authservice.admin.AdminLoginService
import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.oauth.google.GoogleIdTokenVerifierService
import com.example.authservice.oauth.grant.GrantSupport
import com.example.authservice.oauth.grant.PublicClientAuthenticationConverter
import com.example.authservice.oauth.grant.PublicClientAuthenticationProvider
import com.example.authservice.oauth.grant.admin.AdminPasswordGrantConverter
import com.example.authservice.oauth.grant.admin.AdminPasswordGrantProvider
import com.example.authservice.oauth.grant.google.GoogleIdTokenGrantConverter
import com.example.authservice.oauth.grant.google.GoogleIdTokenGrantProvider
import com.example.authservice.oauth.grant.sso.SsoCodeGrantConverter
import com.example.authservice.oauth.grant.sso.SsoCodeGrantProvider
import com.example.authservice.oauth.sso.SsoCodeStore
import com.example.authservice.oauth.store.RedisOAuth2AuthorizationService
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.HexFormat
import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain

/**
 * auth-service 를 OAuth 2.0 / OIDC 인증 서버로 만든다. 브라우저 인가 코드 흐름은 쓰지 않고
 * 커스텀 grant 세 개(구글 ID 토큰, 앱 간 SSO 코드, 관리자 비밀번호)로 토큰을 발급한다.
 */
@Configuration
@EnableWebSecurity
class AuthorizationServerConfig(private val props: OAuthProperties) {

    companion object {
        @JvmField
        val GOOGLE_ID_TOKEN = AuthorizationGrantType("urn:modu:params:oauth:grant-type:google_id_token")

        @JvmField
        val SSO_CODE = AuthorizationGrantType("urn:modu:params:oauth:grant-type:sso_code")

        @JvmField
        val ADMIN_PASSWORD = AuthorizationGrantType("urn:modu:params:oauth:grant-type:admin_password")

        /** 설정의 grants 이름을 AuthorizationGrantType 으로. 모르는 이름은 기동 실패. */
        internal fun grantOf(name: String): AuthorizationGrantType = when (name) {
            "google_id_token" -> GOOGLE_ID_TOKEN
            "sso_code" -> SSO_CODE
            "admin_password" -> ADMIN_PASSWORD
            "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN
            else -> throw IllegalArgumentException("지원하지 않는 grant: $name")
        }

        private fun pem(pemText: String?, label: String): ByteArray {
            if (pemText == null) {
                val key = if (label.startsWith("PRIVATE")) "private-key" else "public-key"
                throw IllegalStateException("modu.oauth.rsa.$key 가 없습니다.")
            }
            val body = pemText.replace("-----BEGIN $label-----", "").replace("-----END $label-----", "").replace(Regex("\\s"), "")
            return Base64.getDecoder().decode(body)
        }
    }

    @Bean
    @Order(1)
    @Throws(Exception::class)
    fun authorizationServerChain(
        http: HttpSecurity,
        clients: RegisteredClientRepository,
        authorizationService: OAuth2AuthorizationService,
        tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>,
        verifier: GoogleIdTokenVerifierService,
        members: MemberFeignClient,
        ssoCodeStore: SsoCodeStore,
        adminLoginService: AdminLoginService,
    ): SecurityFilterChain {
        val authorizationServer = OAuth2AuthorizationServerConfigurer.authorizationServer()
        val support = GrantSupport(tokenGenerator, authorizationService)

        http.securityMatcher(authorizationServer.endpointsMatcher)
            .with(authorizationServer) { c ->
                c.clientAuthentication { ca ->
                    ca.authenticationConverter(PublicClientAuthenticationConverter())
                        .authenticationProvider(PublicClientAuthenticationProvider(clients))
                }
                    .tokenEndpoint { t ->
                        t.accessTokenRequestConverters { list ->
                            list.add(GoogleIdTokenGrantConverter())
                            list.add(SsoCodeGrantConverter())
                            list.add(AdminPasswordGrantConverter())
                        }
                            .authenticationProviders { list ->
                                list.add(GoogleIdTokenGrantProvider(verifier, members, support))
                                list.add(SsoCodeGrantProvider(ssoCodeStore, members, support))
                                list.add(AdminPasswordGrantProvider(adminLoginService, support))
                            }
                    }
                    .oidc { oidc ->
                        oidc
                            // 메타데이터에 커스텀 grant 를 알린다. 라이브러리는 표준 grant 만 나열한다.
                            .providerConfigurationEndpoint { p ->
                                p.providerConfigurationCustomizer { b ->
                                    b.grantType(GOOGLE_ID_TOKEN.value)
                                        .grantType(SSO_CODE.value)
                                        .grantType(ADMIN_PASSWORD.value)
                                }
                            }
                            .userInfoEndpoint { u ->
                                u.userInfoMapper { ctx ->
                                    val sub = ctx.authorization.principalName
                                    val m = members.getMember(sub)
                                    val claims = HashMap<String, Any>()
                                    claims["sub"] = sub
                                    claims["name"] = m?.username ?: ""
                                    claims["email"] = m?.email ?: ""
                                    claims["picture"] = m?.profileImage ?: ""
                                    OidcUserInfo(claims)
                                }
                            }
                    }
            }
            .authorizeHttpRequests { a -> a.anyRequest().authenticated() }
            .csrf { csrf -> csrf.ignoringRequestMatchers(authorizationServer.endpointsMatcher) }
            // Bearer 토큰은 /userinfo 에서만 읽는다. 앱이 만료된 액세스 토큰을 기본 헤더로 달고 /oauth2/token 을
            // 불러도(재로그인·재발급) 리소스 서버가 먼저 401 을 내지 않게 한다.
            .oauth2ResourceServer { rs ->
                rs.bearerTokenResolver { request ->
                    if (request.requestURI.endsWith("/userinfo")) DefaultBearerTokenResolver().resolve(request) else null
                }
                    .jwt(Customizer.withDefaults())
            }
        return http.build()
    }

    /** 나머지 경로(내부 API, sso-code, actuator): 게이트웨이·내부 필터가 지키므로 permitAll, 세션 없음. */
    @Bean
    @Order(2)
    @Throws(Exception::class)
    fun defaultChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .cors { it.disable() }
            .sessionManagement { s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { a -> a.anyRequest().permitAll() }
        return http.build()
    }

    @Bean
    fun registeredClientRepository(): RegisteredClientRepository {
        val clients = props.clients.map { c ->
            val id = requireNotNull(c.id) { "modu.oauth.clients[].id 가 없습니다." }
            val b = RegisteredClient.withId(UUID.nameUUIDFromBytes(id.toByteArray()).toString())
                .clientId(id)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).requireProofKey(true).build())
                .tokenSettings(
                    TokenSettings.builder()
                        .accessTokenTimeToLive(props.accessTokenTtl)
                        .refreshTokenTimeToLive(props.refreshTokenTtl)
                        .reuseRefreshTokens(false)
                        .build(),
                )
            c.grants.forEach { g -> b.authorizationGrantType(grantOf(g)) }
            c.scopes.forEach { b.scope(it) }
            b.build()
        }
        return InMemoryRegisteredClientRepository(clients)
    }

    @Bean
    @Throws(Exception::class)
    fun jwkSource(): JWKSource<SecurityContext> {
        val kf = KeyFactory.getInstance("RSA")
        val priv = kf.generatePrivate(PKCS8EncodedKeySpec(pem(props.rsa.privateKey, "PRIVATE KEY"))) as RSAPrivateKey
        val pub = kf.generatePublic(X509EncodedKeySpec(pem(props.rsa.publicKey, "PUBLIC KEY"))) as RSAPublicKey
        val kid = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pub.encoded)).substring(0, 16)
        val key = RSAKey.Builder(pub).privateKey(priv).keyID(kid).build()
        return ImmutableJWKSet(JWKSet(key))
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder =
        OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings =
        AuthorizationServerSettings.builder().issuer(props.issuer).build()

    @Bean
    fun authorizationService(redis: StringRedisTemplate, clients: RegisteredClientRepository): OAuth2AuthorizationService =
        RedisOAuth2AuthorizationService(redis, clients)

    @Bean
    fun tokenGenerator(
        jwkSource: JWKSource<SecurityContext>,
        customizer: OAuth2TokenCustomizer<JwtEncodingContext>,
    ): OAuth2TokenGenerator<out OAuth2Token> {
        val jwt = JwtGenerator(NimbusJwtEncoder(jwkSource))
        jwt.setJwtCustomizer(customizer)
        return DelegatingOAuth2TokenGenerator(jwt, OAuth2AccessTokenGenerator(), OAuth2RefreshTokenGenerator())
    }

    /** 액세스 토큰에 roles 를 넣는다. 게이트웨이가 ROLE_* 로 라우트를 지킨다. */
    @Bean
    fun rolesCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> = OAuth2TokenCustomizer { ctx ->
        if (OAuth2TokenType.ACCESS_TOKEN == ctx.tokenType) {
            val roles = ctx.getPrincipal<org.springframework.security.core.Authentication>().authorities.map(GrantedAuthority::getAuthority)
            ctx.claims.claim("roles", roles)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
