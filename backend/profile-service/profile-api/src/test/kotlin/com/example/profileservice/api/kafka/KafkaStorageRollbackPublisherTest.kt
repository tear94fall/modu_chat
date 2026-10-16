package com.example.profileservice.api.kafka

import com.example.profileservice.api.logging.RequestContext
import com.example.profileservice.application.domain.entity.ProfileType
import com.example.profileservice.application.usecase.result.ProfileResult
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * 발행 결과 콜백(whenComplete)은 Kafka 네트워크 스레드에서 돌아 MDC 가 비어 있다. 보낼 때의 requestId·userId 를 잡아 두고
 * 콜백 로그에 되살려야 "sent successfully / failed" 줄이 어느 요청 것인지 OpenSearch 에서 찾을 수 있다(2026-10-16 확인: 90건 모두 requestId 없음).
 */
class KafkaStorageRollbackPublisherTest {

    private val kafkaTemplate: KafkaTemplate<String, Any> = mock()
    private val publisher = KafkaStorageRollbackPublisher(kafkaTemplate)
    private val logger = LoggerFactory.getLogger(KafkaStorageRollbackPublisher::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setUp() {
        appender.start()
        logger.addAppender(appender)
        MDC.put(RequestContext.MDC_REQUEST_ID, "req-cb-9")
        MDC.put(RequestContext.MDC_USER_ID, "11")
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(appender)
        MDC.clear()
    }

    /** send() 가 돌려줄 미래값. 콜백은 요청 스레드 밖(다른 스레드, MDC 없음)에서 완료시킨다 — 실제 Kafka 네트워크 스레드와 같은 조건. */
    private fun pendingSend(): CompletableFuture<SendResult<String, Any>> {
        val future = CompletableFuture<SendResult<String, Any>>()
        whenever(kafkaTemplate.send(any<ProducerRecord<String, Any>>())).thenReturn(future)
        return future
    }

    private fun onAnotherThreadWithoutMdc(block: () -> Unit) {
        Thread { MDC.clear(); block() }.also { it.start(); it.join(5_000) }
    }

    private fun awaitLog(): ILoggingEvent {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (appender.list.isEmpty() && System.nanoTime() < deadline) Thread.sleep(10)
        return appender.list.single()
    }

    @Test
    fun successCallbackLogCarriesRequestIdAndUserIdOfTheSendingRequest() {
        val future = pendingSend()
        publisher.publish("1", ProfileResult(null, 1L, ProfileType.PROFILE_IMAGE, "a.png", null, null))
        val record = ProducerRecord<String, Any>("topic-x", "k", mapOf("value" to "a.png"))
        onAnotherThreadWithoutMdc { future.complete(SendResult(record, RecordMetadata(TopicPartition("topic-x", 0), 0, 0, 0, 0, 0))) }

        val event = awaitLog()
        assertTrue(event.formattedMessage.contains("Message sent successfully"), event.formattedMessage)
        assertEquals("req-cb-9", event.mdcPropertyMap[RequestContext.MDC_REQUEST_ID])
        assertEquals("11", event.mdcPropertyMap[RequestContext.MDC_USER_ID])
    }

    @Test
    fun failureCallbackLogCarriesRequestIdOfTheSendingRequest() {
        val future = pendingSend()
        publisher.publish("1", ProfileResult(null, 1L, ProfileType.PROFILE_IMAGE, "a.png", null, null))
        onAnotherThreadWithoutMdc { future.completeExceptionally(RuntimeException("kafka down")) }

        val event = awaitLog()
        assertTrue(event.formattedMessage.contains("Message sent failed"), event.formattedMessage)
        assertEquals("req-cb-9", event.mdcPropertyMap[RequestContext.MDC_REQUEST_ID])
    }
}
