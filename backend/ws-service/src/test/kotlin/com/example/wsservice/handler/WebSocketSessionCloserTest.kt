package com.example.wsservice.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle
import org.springframework.context.SmartLifecycle
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class WebSocketSessionCloserTest {

    private val registry = ConcurrentHashMap<String, WebSocketSession>()

    private fun closer(waitMillis: Long = 100) = WebSocketSessionCloser({ registry.values }, waitMillis).also { it.start() }

    private fun session(id: String, open: Boolean = true): WebSocketSession {
        val s = mock<WebSocketSession>()
        whenever(s.id).thenReturn(id)
        whenever(s.isOpen).thenReturn(open)
        registry[id] = s
        return s
    }

    @Test
    @DisplayName("stop 은 열린 세션을 전부 1001(GOING_AWAY)로 닫는다")
    fun stop_closesOpenSessionsWithGoingAway() {
        val a = session("a")
        val b = session("b")

        closer().stop()

        verify(a).close(CloseStatus.GOING_AWAY)
        verify(b).close(CloseStatus.GOING_AWAY)
    }

    @Test
    @DisplayName("이미 닫힌 세션은 건드리지 않는다")
    fun stop_skipsClosedSessions() {
        val closed = session("closed", open = false)

        closer().stop()

        verify(closed, never()).close(CloseStatus.GOING_AWAY)
    }

    @Test
    @DisplayName("한 세션의 close 가 실패해도 나머지는 닫는다")
    fun stop_continuesWhenOneCloseFails() {
        val bad = session("bad")
        doThrow(IOException("broken pipe")).whenever(bad).close(CloseStatus.GOING_AWAY)
        val good = session("good")

        closer().stop()

        verify(good).close(CloseStatus.GOING_AWAY)
    }

    @Test
    @DisplayName("세션 목록이 비면 바로 돌아오고, 안 비면 waitMillis 뒤에 돌아온다")
    fun stop_waitsForDrainButNotForever() {
        val drained = session("drained")
        // 앱이 close 에 응답하면 afterConnectionClosed 가 목록에서 지운다 — close 가 호출되면 지워지는 걸 흉내 낸다.
        doAnswer { registry.remove("drained"); null }.whenever(drained).close(CloseStatus.GOING_AWAY)
        var elapsed = timed { closer(waitMillis = 1_000).stop() }
        assertThat(elapsed).isLessThan(500)

        registry.clear()
        session("stuck")
        elapsed = timed { closer(waitMillis = 200).stop() }
        assertThat(elapsed).isBetween(200, 2_000)
    }

    @Test
    @DisplayName("stop 은 한 번만 동작하고, 그 뒤 isRunning 은 false 다")
    fun stop_isIdempotent() {
        val s = session("once")
        val c = closer()

        c.stop()
        c.stop()

        verify(s).close(CloseStatus.GOING_AWAY)
        assertThat(c.isRunning).isFalse()
    }

    @Test
    @DisplayName("phase 가 웹 서버 graceful shutdown 보다 높아 서버가 새 요청을 거절하기 전에 먼저 멈춘다")
    fun phase_isBeforeWebServerGracefulShutdown() {
        val c = closer()
        assertThat(c.phase).isGreaterThan(WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE)
        assertThat(c.phase).isEqualTo(SmartLifecycle.DEFAULT_PHASE)
        assertThat(c.isAutoStartup).isTrue()
    }

    private fun timed(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000
    }
}
