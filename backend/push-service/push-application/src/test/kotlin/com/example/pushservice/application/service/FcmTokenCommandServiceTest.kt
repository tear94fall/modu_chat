package com.example.pushservice.application.service

import com.example.pushservice.application.domain.entity.FcmToken
import com.example.pushservice.application.domain.repository.ro.FcmTokenRoRepository
import com.example.pushservice.application.domain.repository.rw.FcmTokenRwRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FcmTokenCommandServiceTest {

    @Test
    fun saveFcmToken_updatesExistingRowInsteadOfInserting() {
        val repository = mock<FcmTokenRwRepository>()
        val service = FcmTokenCommandService(repository)

        val existing = FcmToken("u1", "oldToken").apply { id = 10L }
        whenever(repository.findFirstByUserIdOrderByIdDesc("u1")).thenReturn(Optional.of(existing))
        whenever(repository.save(any<FcmToken>())).thenAnswer { it.getArgument(0) }

        val saved = service.saveFcmToken("u1", "newToken")

        assertEquals("newToken", saved.fcmToken)
        assertEquals(10L, saved.id)
        assertEquals("newToken", existing.fcmToken)
        verify(repository).save(existing)
        verify(repository, never()).save(argThat<FcmToken> { this !== existing })
        verify(repository).deleteByUserIdAndIdNot("u1", 10L)
    }

    @Test
    fun saveFcmToken_insertsWhenNoneExists() {
        val repository = mock<FcmTokenRwRepository>()
        val service = FcmTokenCommandService(repository)

        whenever(repository.findFirstByUserIdOrderByIdDesc("u2")).thenReturn(Optional.empty())
        whenever(repository.save(any<FcmToken>())).thenAnswer { (it.getArgument(0) as FcmToken).apply { id = 20L } }

        val saved = service.saveFcmToken("u2", "brandNewToken")

        assertEquals("u2", saved.userId)
        assertEquals("brandNewToken", saved.fcmToken)
        verify(repository).save(argThat<FcmToken> { userId == "u2" && fcmToken == "brandNewToken" })
        verify(repository).deleteByUserIdAndIdNot("u2", 20L)
    }

    @Test
    fun searchFcmToken_returnsNullWhenMissing() {
        val repository = mock<FcmTokenRoRepository>()
        val service = FcmTokenQueryService(repository)

        whenever(repository.findFirstByUserIdOrderByIdDesc("nobody")).thenReturn(Optional.empty())

        assertNull(service.searchFcmToken("nobody"))
    }
}
