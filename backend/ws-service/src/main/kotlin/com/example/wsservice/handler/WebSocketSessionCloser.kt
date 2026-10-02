package com.example.wsservice.handler

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 종료(SIGTERM, 롤링 배포) 때 열린 WebSocket 세션을 전부 1001(GOING_AWAY)로 닫는다.
 *
 * 그냥 프로세스가 죽으면 앱의 OkHttp 는 ping 타임아웃(20s 간격)까지 끊긴 줄 모른다. close 프레임을 보내면
 * 앱이 onClosing 에서 1000 으로 응답하고 onClosed 에서 재접속을 예약한다(android/modu-chat OkHttpChatSocket).
 *
 * SmartLifecycle 의 phase 를 웹 서버 graceful shutdown([WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE])과
 * Kafka 리스너(DEFAULT_PHASE - 100)보다 높게 둬, 서버가 새 요청을 거절하기 전에 먼저 돈다(stop 은 phase 가 높은 것부터).
 * 닫은 뒤에는 세션 목록이 비워질 때까지([WebSocketHandler.afterConnectionClosed] 가 지운다) 최대 [waitMillis] 만 기다린다.
 */
@Component
class WebSocketSessionCloser(
    private val sessions: () -> Collection<WebSocketSession>,
    private val waitMillis: Long = DEFAULT_WAIT_MILLIS,
) : SmartLifecycle {

    @Autowired
    constructor(handler: WebSocketHandler) : this({ handler.clients.values })

    private val log = LoggerFactory.getLogger(WebSocketSessionCloser::class.java)
    private val running = AtomicBoolean(false)

    override fun start() {
        running.set(true)
    }

    override fun stop() {
        if (!running.compareAndSet(true, false)) return
        val open = sessions().filter { it.isOpen }
        if (open.isEmpty()) return

        log.info("[ws] shutting down: closing {} session(s) with {}", open.size, CloseStatus.GOING_AWAY)
        open.forEach { session ->
            try {
                session.close(CloseStatus.GOING_AWAY)
            } catch (e: Exception) {
                log.warn("[ws] failed to close session {} on shutdown: {}", session.id, e.message)
            }
        }
        awaitDrain()
    }

    /** 세션 목록이 비거나 [waitMillis] 가 지나면 돌아온다. 앱이 close 응답을 안 해도 종료를 막지 않는다. */
    private fun awaitDrain() {
        val deadline = System.nanoTime() + waitMillis * 1_000_000
        while (sessions().isNotEmpty() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(POLL_MILLIS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }
        val left = sessions().size
        if (left > 0) log.info("[ws] {} session(s) still registered after {}ms; continuing shutdown", left, waitMillis)
    }

    override fun isRunning(): Boolean = running.get()

    override fun isAutoStartup(): Boolean = true

    override fun getPhase(): Int = PHASE

    companion object {
        const val DEFAULT_WAIT_MILLIS = 2_000L
        private const val POLL_MILLIS = 50L

        /** 웹 서버 graceful shutdown(MAX-1024)·Kafka 리스너(MAX-100)보다 먼저 멈춘다. */
        const val PHASE = SmartLifecycle.DEFAULT_PHASE

        init {
            check(PHASE > WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE)
        }
    }
}
