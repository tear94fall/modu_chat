package com.example.storageservice.kafka.config

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

@EnableKafka
@Configuration
class KafkaConsumerConfig(
    @Value("\${spring.kafka.consumer.bootstrap-servers}") private val bootstrapServers: String,
    @Value("\${spring.kafka.consumer.group-id}") private val groupId: String,
) {

    private val log = LoggerFactory.getLogger(KafkaConsumerConfig::class.java)

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val config = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 500,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to 5000,
        )
        val deserializer = JsonDeserializer(Any::class.java, false)
        return DefaultKafkaConsumerFactory(config, StringDeserializer(), deserializer)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.setConcurrency(3)
        factory.consumerFactory = consumerFactory()
        factory.setCommonErrorHandler(defaultErrorHandler())
        return factory
    }

    @Bean
    fun defaultErrorHandler(): DefaultErrorHandler =
        DefaultErrorHandler({ record, _ -> log.error("Error Data: {}", record.toString()) }, FixedBackOff(1000, 5))
}
