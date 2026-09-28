package com.example.authservice.oauth.usage

import com.example.authservice.member.client.MemberUsageFeignClient
import com.example.authservice.member.dto.UsageRequest
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/** 처리기·알림기 단독: 원래 응답을 먼저 쓰고, 이용 기록 쪽 예외는 밖으로 새지 않는다. */
class UsageRecordingTokenResponseHandlerTest {

    private val direct = Executor { it.run() }

    @Test
    fun 알림기는_클라이언트_실패와_큐_거절을_삼킨다() {
        val client = mock<MemberUsageFeignClient>()
        doThrow(RuntimeException("down")).whenever(client).recordUsage(any())
        assertThatCode { MemberUsageNotifier(client, direct).notify("u", "modu-chat") }.doesNotThrowAnyException()
        verify(client).recordUsage(UsageRequest("u", "modu-chat"))

        val rejecting = Executor { throw RejectedExecutionException("full") }
        assertThatCode { MemberUsageNotifier(client, rejecting).notify("u", "modu-chat") }.doesNotThrowAnyException()
    }

    @Test
    fun 대상이_아닌_grant_나_토큰이_아니면_원래_응답만_쓰고_기록하지_않는다() {
        val delegate = mock<AuthenticationSuccessHandler>()
        val client = mock<MemberUsageFeignClient>()
        val handler = UsageRecordingTokenResponseHandler(delegate, MemberUsageNotifier(client, direct), setOf("refresh_token"))
        val request = MockHttpServletRequest().apply { addParameter("grant_type", "client_credentials") }
        val response = MockHttpServletResponse()
        val auth = TestingAuthenticationToken("x", null)

        handler.onAuthenticationSuccess(request, response, auth)
        request.setParameter("grant_type", "refresh_token")
        assertThatCode { handler.onAuthenticationSuccess(request, response, auth) }.doesNotThrowAnyException()

        verify(delegate, org.mockito.kotlin.times(2)).onAuthenticationSuccess(request, response, auth)
        verify(client, never()).recordUsage(any())
    }
}
