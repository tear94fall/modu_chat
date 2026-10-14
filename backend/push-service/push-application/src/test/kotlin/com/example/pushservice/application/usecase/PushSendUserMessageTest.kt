package com.example.pushservice.application.usecase

import com.example.pushservice.application.push.PushSender
import com.example.pushservice.application.service.FcmTokenQueryService
import com.example.pushservice.application.usecase.command.UserDataPushCommand
import com.example.pushservice.application.usecase.result.FcmTokenResult
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 반응 알림처럼 한 사람에게만 보내는 푸시. */
class PushSendUserMessageTest {

    private val queryService = mock<FcmTokenQueryService>()
    private val pushSender = mock<PushSender>()
    private val useCase = PushSendUseCase(queryService, pushSender)

    @Test
    fun sendsToTheUsersTokenWithData() {
        whenever(queryService.searchFcmToken("author")).thenReturn(FcmTokenResult(1L, "author", "tok"))

        val sent = useCase.sendUserData(UserDataPushCommand("author", "방", "준섭님이 👍 반응을 남겼습니다", mapOf("roomId" to "r1")))

        assertTrue(sent)
        verify(pushSender).sendToToken(
            eq("tok"), isNull(), eq(mapOf("title" to "방", "message" to "준섭님이 👍 반응을 남겼습니다", "roomId" to "r1")),
        )
    }

    @Test
    fun withoutTokenSendsNothing() {
        whenever(queryService.searchFcmToken("nobody")).thenReturn(null)

        assertFalse(useCase.sendUserData(UserDataPushCommand("nobody", "방", "b", emptyMap())))
        verify(pushSender, never()).sendToToken(anyOrNull(), anyOrNull(), any())
    }
}
