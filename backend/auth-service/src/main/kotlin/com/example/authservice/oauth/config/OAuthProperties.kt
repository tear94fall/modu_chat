package com.example.authservice.oauth.config

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

/** modu.oauth.* — 발급자, TTL, 등록 클라이언트, 구글 audience, 서명 키. config-repo 가 내려준다. */
@ConfigurationProperties(prefix = "modu.oauth")
class OAuthProperties {
    var issuer: String? = null
    var accessTokenTtl: Duration = Duration.ofHours(1)
    var refreshTokenTtl: Duration = Duration.ofDays(7)
    var ssoCodeTtl: Duration = Duration.ofSeconds(60)
    var google: Google = Google()
    var rsa: Rsa = Rsa()
    var clients: MutableList<Client> = mutableListOf()

    class Google {
        var audiences: MutableList<String> = mutableListOf()
    }

    class Rsa {
        var privateKey: String? = null
        var publicKey: String? = null
    }

    class Client {
        var id: String? = null
        var grants: MutableList<String> = mutableListOf()
        var scopes: MutableList<String> = mutableListOf()

        /** true 면 이 클라이언트로 로그인한 사용자가 다른 앱에 SSO 코드를 발급해 줄 수 있다. */
        var ssoIssuer: Boolean = false
    }

    fun client(id: String?): Client? = clients.firstOrNull { it.id == id }
}
