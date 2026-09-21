package com.example.pushservice.fcm.service

import com.example.pushservice.fcm.entity.FcmToken
import com.example.pushservice.fcm.repository.FcmRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FcmServiceTokenTest {

    @Test
    fun saveFcmToken_updatesExistingRowInsteadOfInserting() {
        val repository = mock<FcmRepository>()
        val service = FcmService(repository)

        val existing = FcmToken("u1", "oldToken").apply { id = 10L }
        whenever(repository.findFirstByUserIdOrderByIdDesc("u1")).thenReturn(Optional.of(existing))
        whenever(repository.save(any<FcmToken>())).thenAnswer { it.getArgument(0) }

        val incoming = FcmToken("u1", "newToken")
        val saved = service.saveFcmToken(incoming)

        assertEquals("newToken", saved.fcmToken)
        assertEquals(existing, saved)
        verify(repository).save(existing)
        verify(repository, never()).save(incoming)
        verify(repository).deleteByUserIdAndIdNot("u1", 10L)
    }

    @Test
    fun saveFcmToken_insertsWhenNoneExists() {
        val repository = mock<FcmRepository>()
        val service = FcmService(repository)

        whenever(repository.findFirstByUserIdOrderByIdDesc("u2")).thenReturn(Optional.empty())
        val incoming = FcmToken("u2", "brandNewToken")
        whenever(repository.save(incoming)).thenReturn(incoming)
        incoming.id = 20L

        val saved = service.saveFcmToken(incoming)

        assertEquals(incoming, saved)
        verify(repository).save(incoming)
        verify(repository).deleteByUserIdAndIdNot("u2", 20L)
    }

    @Test
    fun searchFcmToken_returnsNullWhenMissing() {
        val repository = mock<FcmRepository>()
        val service = FcmService(repository)

        whenever(repository.findFirstByUserIdOrderByIdDesc("nobody")).thenReturn(Optional.empty())

        assertNull(service.searchFcmToken("nobody"))
    }
}
