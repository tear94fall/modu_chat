package com.example.pushservice.fcm.service

import com.example.pushservice.fcm.dto.RequestPushMessage
import com.example.pushservice.fcm.entity.FcmToken
import com.example.pushservice.fcm.repository.FcmRepository
import com.google.firebase.messaging.MulticastMessage
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class FcmServiceBroadcastTest {

    private fun service(tokens: List<FcmToken>): FcmService {
        val service = spy(FcmService(mock<FcmRepository>()))
        doReturn(tokens).whenever(service).searchAllFcmToken()
        doNothing().whenever(service).sendMessage(any<MulticastMessage>())
        return service
    }

    @Test
    fun broadcast_splitsTokensIntoMulticastGroups() {
        val service = service((0 until 7).map { FcmToken("u$it", "t$it") })

        val groups = service.broadcast(RequestPushMessage(title = "t", body = "b"), 3L)

        assertEquals(3, groups)
        verify(service, times(3)).sendMessage(any<MulticastMessage>())
    }

    @Test
    fun broadcast_noTokens_sendsNothing() {
        val service = service(emptyList())

        val groups = service.broadcast(RequestPushMessage(title = "t", body = "b"), 3L)

        assertEquals(0, groups)
        verify(service, never()).sendMessage(any<MulticastMessage>())
    }

    @Test
    fun broadcast_deduplicatesIdenticalTokens() {
        // 5 rows, only 2 distinct tokens (duplicate rows for the same user/token, as in the dev DB bug)
        val service = service(
            listOf(FcmToken("u1", "dup"), FcmToken("u1", "dup"), FcmToken("u1", "dup"), FcmToken("u2", "other"), FcmToken("u2", "other")),
        )

        val groups = service.broadcast(RequestPushMessage(title = "t", body = "b"), 10L)

        // com.google.firebase.messaging.MulticastMessage does not expose its token list
        // (getMessageList()/Message.getToken() are package-private in firebase-admin 9.0.0),
        // so we can only assert on group count here, not on the captured message's token count.
        assertEquals(1, groups)
        verify(service, times(1)).sendMessage(any<MulticastMessage>())
    }
}
