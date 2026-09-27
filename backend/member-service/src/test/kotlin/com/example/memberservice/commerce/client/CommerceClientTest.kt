package com.example.memberservice.commerce.client

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException

/** 실제 로컬 HTTP 서버로 커머스 호출의 모양(DELETE·경로·내부 토큰)과 실패·시간 초과를 확인한다. */
class CommerceClientTest {

    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        executor = Executors.newCachedThreadPool()
        start()
    }
    private val baseUrl = "http://127.0.0.1:${server.address.port}"
    private val release = CountDownLatch(1)

    @AfterEach
    fun stop() {
        release.countDown()
        server.stop(0)
    }

    @Test
    fun deleteCustomer_sendsDeleteWithInternalToken() {
        val method = AtomicReference<String>()
        val path = AtomicReference<String>()
        val token = AtomicReference<String>()
        server.createContext("/") { exchange ->
            method.set(exchange.requestMethod)
            path.set(exchange.requestURI.path)
            token.set(exchange.requestHeaders.getFirst("X-Internal-Token"))
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }

        CommerceClient(baseUrl, "secret-token", Duration.ofSeconds(3)).deleteCustomer("google-sub-1")

        assertThat(method.get()).isEqualTo("DELETE")
        assertThat(path.get()).isEqualTo("/api-internal/v1/customers/google-sub-1")
        assertThat(token.get()).isEqualTo("secret-token")
    }

    @Test
    fun deleteCustomer_throwsOnServerError() {
        server.createContext("/") { exchange ->
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        }

        assertThatThrownBy { CommerceClient(baseUrl, "t", Duration.ofSeconds(3)).deleteCustomer("u") }
            .isInstanceOf(RestClientResponseException::class.java)
    }

    @Test
    fun deleteCustomer_timesOut() {
        server.createContext("/") { exchange ->
            release.await(10, TimeUnit.SECONDS) // 응답하지 않고 붙잡아 둔다
            exchange.close()
        }

        val started = System.nanoTime()
        assertThatThrownBy { CommerceClient(baseUrl, "t", Duration.ofMillis(300)).deleteCustomer("u") }
            .isInstanceOf(ResourceAccessException::class.java)
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(3))
    }

    @Test
    fun defaultTimeout_isThreeSeconds() {
        assertThat(CommerceClient.DEFAULT_TIMEOUT).isEqualTo(Duration.ofSeconds(3))
    }
}
