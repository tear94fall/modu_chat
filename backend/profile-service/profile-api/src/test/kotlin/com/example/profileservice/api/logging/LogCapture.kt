package com.example.profileservice.api.logging

// boot4-src: keep-jackson2 — logstash-logback-encoder 의 StructuredArgument 는 Jackson 2 의 JsonGenerator 에 쓴다. 이 테스트는 Jackson 2 매퍼를 그대로 쓴다.
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import net.logstash.logback.argument.StructuredArgument
import org.slf4j.LoggerFactory
import java.io.StringWriter

/** 테스트에서 특정 로거의 이벤트를 모은다. 구조화 인자(kv)는 LogstashEncoder 가 쓰는 그대로 JSON 으로 풀어 본다. */
class LogCapture(
    loggerName: String,
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(loggerName) as Logger
    private val appender = ListAppender<ILoggingEvent>().apply { start() }

    init {
        logger.addAppender(appender)
    }

    val events: List<ILoggingEvent> get() = appender.list

    override fun close() {
        logger.detachAppender(appender)
        appender.stop()
    }

    companion object {
        private val mapper = ObjectMapper()

        /** 이벤트의 kv 인자를 `{필드: 값}` 으로. */
        fun fields(event: ILoggingEvent): Map<String, Any?> {
            val out = StringWriter()
            mapper.factory.createGenerator(out).use { gen ->
                gen.writeStartObject()
                event.argumentArray
                    .orEmpty()
                    .filterIsInstance<StructuredArgument>()
                    .forEach { it.writeTo(gen) }
                gen.writeEndObject()
            }
            @Suppress("UNCHECKED_CAST")
            return mapper.readValue(out.toString(), Map::class.java) as Map<String, Any?>
        }
    }
}
