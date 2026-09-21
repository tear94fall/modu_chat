package com.example.profileservice.kafka.config

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaProducerConfig(@Value("\${spring.kafka.producer.bootstrap-servers}") private val bootstrapServers: String) {

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val config = mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 1,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 5,
            ProducerConfig.MAX_BLOCK_MS_CONFIG to 3000,
            ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG to 11000,
            // delivery.timeout.ms >= linger.ms + retry.backoff.ms + request.timeout.ms
            ProducerConfig.LINGER_MS_CONFIG to 50,
            ProducerConfig.RETRY_BACKOFF_MS_CONFIG to 5000,
            ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG to 5000,
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> = KafkaTemplate(producerFactory())
}
