package com.example.pushservice.fcm.service

import com.example.pushservice.fcm.dto.FcmUserMessageDto
import com.example.pushservice.fcm.entity.FcmToken
import com.example.pushservice.fcm.repository.FcmRepository
import com.google.firebase.messaging.Message
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 반응 알림처럼 한 사람에게만 보내는 푸시. */
class FcmServiceUserMessageTest {

    @Test
    fun sendsToTheUsersTokenWithData() {
        val repository = mock<FcmRepository>()
        whenever(repository.findFirstByUserIdOrderByIdDesc("author")).thenReturn(Optional.of(FcmToken("author", "tok")))
        val service = spy(FcmService(repository))
        doNothing().whenever(service).sendMessage(any<Message>())

        val sent = service.sendUserMessageWithData(FcmUserMessageDto("author", "방", "준섭님이 👍 반응을 남겼습니다", mapOf("roomId" to "r1")))

        assertTrue(sent)
        verify(service).sendMessage(any<Message>())
    }

    @Test
    fun withoutTokenSendsNothing() {
        val repository = mock<FcmRepository>()
        whenever(repository.findFirstByUserIdOrderByIdDesc("nobody")).thenReturn(Optional.empty())
        val service = spy(FcmService(repository))

        assertFalse(service.sendUserMessageWithData(FcmUserMessageDto("nobody", "방", "b", emptyMap())))
        verify(service, never()).sendMessage(any<Message>())
    }
}
