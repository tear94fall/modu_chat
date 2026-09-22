package com.example.chatservice.kafka.config

import com.example.chatservice.message.entity.ChatMessage
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
    fun consumerFactory(): ConsumerFactory<String, ChatMessage> {
        val config = HashMap<String, Any>()
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        config[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        config[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 500
        config[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = 5000

        val deserializer = JsonDeserializer(ChatMessage::class.java, false)
        deserializer.addTrustedPackages("*")

        return DefaultKafkaConsumerFactory(config, StringDeserializer(), deserializer)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, ChatMessage> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ChatMessage>()
        val containerProperties = factory.containerProperties

        containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE

        factory.setConcurrency(3)
        factory.consumerFactory = consumerFactory()
        factory.setCommonErrorHandler(defaultErrorHandler())

        return factory
    }

    @Bean
    fun defaultErrorHandler(): DefaultErrorHandler {
        val fixedBackOff = FixedBackOff(1000, 5)
        return DefaultErrorHandler({ consumerRecord, _ -> log.error("Error Data: {}", consumerRecord.toString()) }, fixedBackOff)
    }
}
