package com.example.modumessenger.data.repository

import com.example.modumessenger.core.model.Member
import com.example.modumessenger.core.network.ApiException
import com.example.modumessenger.core.network.safeCall
import com.example.modumessenger.core.session.SessionStore
import com.example.modumessenger.core.util.OAuthClient
import com.example.modumessenger.data.api.AuthApi
import com.example.modumessenger.data.api.MemberApi
import com.example.modumessenger.data.dto.SsoCodeRequestDto
import com.example.modumessenger.data.dto.SsoCodeResponseDto
import com.example.modumessenger.data.dto.toModel
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {

    /**
     * 구글 id 토큰을 앱 토큰으로 바꾸고 회원 정보까지 받아 세션에 저장한다.
     * 이메일은 토큰 응답에 없으므로 `GoogleSignInAccount.email` 을 받아 쓴다.
     */
    suspend fun loginWithGoogle(idToken: String, email: String): Result<Member>

    /** revoke 를 시도하되, 성공하든 실패하든 세션은 반드시 지운다. */
    suspend fun logout(): Result<Unit>

    suspend fun issueSsoCode(request: SsoCodeRequestDto): Result<SsoCodeResponseDto>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val memberApi: MemberApi,
    private val sessionStore: SessionStore,
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String, email: String): Result<Member> = safeCall {
        val tokens = authApi.token(OAuthClient.googleForm(idToken))
        val access = tokens.accessToken
        if (access.isNullOrBlank()) throw ApiException(200, "access_token 이 비어 있다")
        sessionStore.saveTokens(access, tokens.refreshToken.orEmpty())

        val member = memberApi.getMemberByEmail(email).toModel()
        sessionStore.saveMember(member)
        member
    }

    override suspend fun logout(): Result<Unit> {
        val refresh = sessionStore.refreshToken()
        if (!refresh.isNullOrBlank()) {
            // 서버가 안 받아도(네트워크 단절 포함) 로그아웃은 계속한다. 기존 앱은 여기서 화면이 멈췄다.
            safeCall { authApi.revoke(refresh, OAuthClient.CLIENT_ID) }
        }
        sessionStore.clearSession()
        return Result.success(Unit)
    }

    override suspend fun issueSsoCode(request: SsoCodeRequestDto): Result<SsoCodeResponseDto> =
        safeCall { authApi.ssoCode(request) }
}
