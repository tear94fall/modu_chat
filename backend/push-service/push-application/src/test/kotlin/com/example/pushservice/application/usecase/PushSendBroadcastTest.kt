package com.example.pushservice.application.usecase

import com.example.pushservice.application.push.PushSender
import com.example.pushservice.application.service.FcmTokenQueryService
import com.example.pushservice.application.usecase.command.NotificationCommand
import com.example.pushservice.application.usecase.result.FcmTokenResult
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class PushSendBroadcastTest {

    private val pushSender = mock<PushSender>()

    private fun useCase(tokens: List<FcmTokenResult>): PushSendUseCase {
        val queryService = mock<FcmTokenQueryService>()
        whenever(queryService.searchAllFcmToken()).thenReturn(tokens)
        return PushSendUseCase(queryService, pushSender)
    }

    private fun token(userId: String, fcmToken: String) = FcmTokenResult(null, userId, fcmToken)

    @Test
    fun broadcast_splitsTokensIntoMulticastGroups() {
        val useCase = useCase((0 until 7).map { token("u$it", "t$it") })

        val groups = useCase.broadcast(NotificationCommand(title = "t", body = "b"), 3L)

        assertEquals(3, groups)
        verify(pushSender, times(3)).sendToTokens(any(), any(), any())
        verify(pushSender).sendToTokens(eq(listOf("t6")), any(), any())
    }

    @Test
    fun broadcast_noTokens_sendsNothing() {
        val useCase = useCase(emptyList())

        val groups = useCase.broadcast(NotificationCommand(title = "t", body = "b"), 3L)

        assertEquals(0, groups)
        verify(pushSender, never()).sendToTokens(any(), any(), any())
    }

    @Test
    fun broadcast_deduplicatesIdenticalTokens() {
        // 5 rows, only 2 distinct tokens (duplicate rows for the same user/token, as in the dev DB bug)
        val useCase = useCase(
            listOf(token("u1", "dup"), token("u1", "dup"), token("u1", "dup"), token("u2", "other"), token("u2", "other")),
        )

        val groups = useCase.broadcast(NotificationCommand(title = "t", body = "b"), 10L)

        assertEquals(1, groups)
        // 포트 뒤로 Firebase 를 숨긴 덕분에 이제 묶음에 실린 토큰까지 확인할 수 있다.
        verify(pushSender, times(1)).sendToTokens(eq(listOf("dup", "other")), any(), any())
    }
}
