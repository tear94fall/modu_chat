package com.example.chatservice.kafka.config

import com.example.chatservice.message.entity.ChatMessage
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaProducerConfig(
    @Value("\${spring.kafka.producer.bootstrap-servers}") protected val bootstrapServers: String,
) {

    @Bean
    fun producerFactory(): ProducerFactory<String, ChatMessage> {
        val config = HashMap<String, Any>()
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java

        config[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.RETRIES_CONFIG] = 1
        config[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = 5

        config[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 3000
        config[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] = 11000
        // delivery.timeout.ms >= linger.ms + retry.backoff.ms + request.timeout.ms
        config[ProducerConfig.LINGER_MS_CONFIG] = 50
        config[ProducerConfig.RETRY_BACKOFF_MS_CONFIG] = 5000
        config[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] = 5000

        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, ChatMessage> = KafkaTemplate(producerFactory())
}
