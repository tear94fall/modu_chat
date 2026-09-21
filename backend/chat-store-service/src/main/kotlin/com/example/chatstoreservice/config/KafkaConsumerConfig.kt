package com.example.chatstoreservice.config

import com.example.chatstoreservice.message.ChatListener
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
@EnableKafka
class KafkaConsumerConfig(
    @Value("\${spring.kafka.consumer.bootstrap-servers}") private val bootstrapServers: String,
    @Value("\${spring.kafka.consumer.group-id}") private val groupId: String,
    @Value("\${spring.kafka.consumer.enable-auto-commit}") private val enableAutoCommit: Boolean,
    @Value("\${spring.kafka.consumer.auto-offset-reset}") private val autoOffsetReset: String,
) {

    private val log = LoggerFactory.getLogger(KafkaConsumerConfig::class.java)

    @Bean
    fun consumerFactory(): ConsumerFactory<String, ChatListener> {
        val config = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to customizedJsonDeserializer(),
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to enableAutoCommit,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to 5000,
        )
        return DefaultKafkaConsumerFactory(config, StringDeserializer(), customizedJsonDeserializer())
    }

    @Bean
    fun chatKafkaListener(): ConcurrentKafkaListenerContainerFactory<String, ChatListener> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ChatListener>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setCommonErrorHandler(
            DefaultErrorHandler({ record, _ -> log.info("consumer retry: {}", record.value()) }, FixedBackOff(1000L, 3L)),
        )
        return factory
    }

    /** Debezium 이벤트에는 모르는 필드가 많다. 전용 ObjectMapper 로 모르는 필드를 무시한다. */
    private fun customizedJsonDeserializer(): JsonDeserializer<ChatListener> {
        val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return JsonDeserializer(ChatListener::class.java, objectMapper).apply {
            setRemoveTypeHeaders(false)
            addTrustedPackages("*")
            setUseTypeMapperForKey(true)
        }
    }
}
