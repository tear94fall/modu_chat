package com.example.authservice.oauth.sso

import com.example.authservice.api.pub.dto.SsoCodeResponse
import com.example.authservice.oauth.config.OAuthProperties
import java.security.SecureRandom
import java.util.Base64
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.server.ResponseStatusException

/** 로그인된 앱(sso-issuer)이 다른 앱을 위해 1회용 코드를 만든다. 코드는 대상 클라이언트와 PKCE 챌린지에 묶인다. */
@Service
class SsoCodeService(
    private val props: OAuthProperties,
    private val store: SsoCodeStore,
) {

    fun issue(issuerClientId: String, sub: String, targetClientId: String?, codeChallenge: String?, method: String?): SsoCodeResponse {
        val issuer = props.client(issuerClientId)
        if (issuer == null || !issuer.ssoIssuer) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "이 앱은 다른 앱에 로그인을 넘겨줄 수 없습니다.")
        }
        val target = props.client(targetClientId)
        if (target == null || !target.grants.contains("sso_code")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SSO 로그인을 받을 수 없는 앱입니다: $targetClientId")
        }
        if (method != "S256" || !StringUtils.hasText(codeChallenge) || codeChallenge!!.length < 43 || codeChallenge.length > 128) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "code_challenge(S256) 가 필요합니다.")
        }
        val bytes = ByteArray(32)
        RANDOM.nextBytes(bytes)
        val code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        store.save(code, SsoCode(sub, targetClientId!!, codeChallenge, method), props.ssoCodeTtl)
        return SsoCodeResponse(code, props.ssoCodeTtl.toSeconds())
    }

    companion object {
        private val RANDOM = SecureRandom()
    }
}
