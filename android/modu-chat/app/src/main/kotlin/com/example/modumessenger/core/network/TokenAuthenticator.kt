package com.example.modumessenger.core.network

import com.example.modumessenger.core.session.SessionEvents
import com.example.modumessenger.core.session.SessionStore
import com.example.modumessenger.core.util.OAuthClient
import com.example.modumessenger.data.api.AuthApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 401 을 받으면 refresh_token 으로 한 번만 갱신하고 요청을 재시도한다.
 *
 * - 단일 비행: 여러 요청이 동시에 401 을 받아도 갱신은 한 번만 한다([mutex]). 뒤늦게 들어온 요청은
 *   이미 바뀐 토큰이 있으면 그것으로만 재시도한다.
 * - 서버가 refresh 토큰을 거절(400·401)하거나 refresh 토큰이 없으면 → 세션을 지우고 [SessionEvents.notifyLoggedOut] 으로
 *   로그인 화면으로 보낸다(로그아웃 구독자가 소켓을 끊고 앱 잠금도 지운다).
 * - 연결 실패·타임아웃·서버 오류(5xx, 게이트웨이 503)는 일시 장애다. 세션은 그대로 두고 이 요청만 원래의 401 로 실패시킨다.
 *   다음 요청의 401 이 다시 갱신을 시도하므로 연결이 돌아오면 저절로 정상화된다.
 *   (예전에는 모든 실패를 로그아웃으로 처리해, 토큰이 만료된 순간 연결이 끊기거나 서버가 재시작 중이면 로그인과 PIN 이 풀렸다.)
 * - 갱신 호출은 인터셉터·Authenticator 가 없는 별도 클라이언트([AuthApi] `@Named("plain")`)로 한다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionStore: SessionStore,
    private val sessionEvents: SessionEvents,
    @Named("plain") private val plainAuthApi: AuthApi,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 이미 한 번 재시도한 요청이면 더 하지 않는다.
        if (response.priorResponse != null) return null

        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        return runBlocking {
            mutex.withLock {
                val current = sessionStore.accessToken()
                if (!current.isNullOrBlank() && current != failedToken) {
                    // 다른 요청이 이미 갱신했다.
                    return@withLock response.request.withToken(current)
                }

                val refresh = sessionStore.refreshToken()
                if (refresh.isNullOrBlank()) {
                    giveUp()
                    return@withLock null
                }

                val tokens =
                    try {
                        plainAuthApi.token(OAuthClient.refreshForm(refresh))
                    } catch (e: HttpException) {
                        if (e.code() == 400 || e.code() == 401) giveUp()
                        return@withLock null
                    } catch (e: Exception) {
                        // 연결 실패·타임아웃 등. 세션은 살아 있다.
                        return@withLock null
                    }
                val newAccess = tokens.accessToken
                if (newAccess.isNullOrBlank()) {
                    giveUp()
                    return@withLock null
                }

                sessionStore.saveTokens(newAccess, tokens.refreshToken ?: refresh)
                response.request.withToken(newAccess)
            }
        }
    }

    private suspend fun giveUp() {
        sessionStore.clearSession()
        sessionEvents.notifyLoggedOut()
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()
}
