package com.example.authservice.oauth.google

import com.example.authservice.oauth.config.OAuthProperties
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.stereotype.Service

/**
 * 구글 ID 토큰 검증. 서명·만료·audience(우리 앱 클라이언트 ID)를 확인한다.
 * 테스트에서 네트워크 없이 목킹할 수 있게 별도 빈으로 둔다.
 */
@Service
class GoogleIdTokenVerifierService(private val props: OAuthProperties) {

    fun verify(idToken: String): GoogleAccount {
        try {
            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(props.google.audiences)
                .build()
            val token = verifier.verify(idToken) ?: throw invalid("구글 ID 토큰이 유효하지 않습니다.")
            val p = token.payload
            return GoogleAccount(
                p.subject,
                p.email,
                p["name"]?.toString() ?: "",
                p["picture"]?.toString() ?: "",
            )
        } catch (e: OAuth2AuthenticationException) {
            throw e
        } catch (e: Exception) {
            throw invalid("구글 ID 토큰을 검증하지 못했습니다.")
        }
    }

    private fun invalid(message: String): OAuth2AuthenticationException =
        OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, message, null))
}
