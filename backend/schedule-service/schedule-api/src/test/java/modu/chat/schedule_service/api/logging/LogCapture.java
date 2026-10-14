package modu.chat.schedule_service.api.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
// logstash-logback-encoder 의 StructuredArgument 는 Jackson 2 의 JsonGenerator 에 쓴다. 이 테스트는 Jackson 2 매퍼를 그대로 쓴다.
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.logback.argument.StructuredArgument;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/** 테스트에서 특정 로거의 이벤트를 모은다. 구조화 인자(kv)는 LogstashEncoder 가 쓰는 그대로 JSON 으로 풀어 본다. */
public class LogCapture implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    public LogCapture(String loggerName) {
        this.logger = (Logger) LoggerFactory.getLogger(loggerName);
        appender.start();
        logger.addAppender(appender);
    }

    public List<ILoggingEvent> events() {
        return appender.list;
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }

    /** 이벤트의 kv 인자를 {@code {필드: 값}} 으로. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> fields(ILoggingEvent event) {
        StringWriter out = new StringWriter();
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            gen.writeStartObject();
            Object[] args = event.getArgumentArray();
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof StructuredArgument structured) {
                        structured.writeTo(gen);
                    }
                }
            }
            gen.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            return MAPPER.readValue(out.toString(), Map.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
