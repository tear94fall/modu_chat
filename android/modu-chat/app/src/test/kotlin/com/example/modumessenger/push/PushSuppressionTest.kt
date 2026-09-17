package com.example.modumessenger.push

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 알림 억제 규칙(스펙 §7 + 설계 §3). */
class PushSuppressionTest {

    @Test
    fun `차단한 사람이 보낸 알림은 띄우지 않는다`() {
        assertTrue(
            PushSuppression.shouldSuppress(
                sender = "blocked",
                myUserId = ME,
                blockedIds = setOf("blocked"),
                isForeground = false,
                isSocketConnected = false,
            ),
        )
    }

    @Test
    fun `차단하지 않은 사람이 보냈으면 띄운다`() {
        assertFalse(
            PushSuppression.shouldSuppress(
                sender = "friend",
                myUserId = ME,
                blockedIds = setOf("blocked"),
                isForeground = false,
                isSocketConnected = false,
            ),
        )
    }

    @Test
    fun `내가 보낸 에코는 띄우지 않는다`() {
        assertTrue(
            PushSuppression.shouldSuppress(
                sender = ME,
                myUserId = ME,
                blockedIds = emptySet(),
                isForeground = false,
                isSocketConnected = false,
            ),
        )
    }

    @Test
    fun `앞에 떠 있고 소켓도 붙어 있으면 배너가 대신 알린다`() {
        assertTrue(
            PushSuppression.shouldSuppress(
                sender = "friend",
                myUserId = ME,
                blockedIds = emptySet(),
                isForeground = true,
                isSocketConnected = true,
            ),
        )
    }

    @Test
    fun `앞에 떠 있어도 소켓이 끊겼으면 띄운다`() {
        assertFalse(
            PushSuppression.shouldSuppress(
                sender = "friend",
                myUserId = ME,
                blockedIds = emptySet(),
                isForeground = true,
                isSocketConnected = false,
            ),
        )
    }

    @Test
    fun `발신자를 모르면 차단 여부를 따지지 않는다`() {
        assertFalse(
            PushSuppression.shouldSuppress(
                sender = null,
                myUserId = ME,
                blockedIds = setOf("blocked"),
                isForeground = false,
                isSocketConnected = false,
            ),
        )
    }

    private companion object {
        const val ME = "me"
    }
}
