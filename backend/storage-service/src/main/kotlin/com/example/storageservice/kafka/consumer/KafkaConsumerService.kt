package com.example.storageservice.kafka.consumer

import com.example.storageservice.kafka.dto.ProfileDto
import com.example.storageservice.kafka.dto.ProfileType
import com.example.storageservice.kafka.producer.KafkaProducerService
import com.example.storageservice.service.StorageService
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

/** 프로필 저장 롤백: member-service 가 실패한 프로필 사진·배경을 지워 달라고 보내면 지우고 member 쪽 롤백 토픽으로 넘긴다. */
@Service
class KafkaConsumerService(
    private val storageService: StorageService,
    private val objectMapper: ObjectMapper,
    private val kafkaProducerService: KafkaProducerService,
) {

    private val log = LoggerFactory.getLogger(KafkaConsumerService::class.java)

    @KafkaListener(topics = ["topic-storage-rollback"], groupId = "\${spring.kafka.consumer.group-id}")
    fun receive(consumerRecord: ConsumerRecord<String, Any>, acknowledgment: Acknowledgment) {
        try {
            val key = consumerRecord.key()
            val payload = consumerRecord.value()
            log.info("received payload = {}", payload.toString())

            val profileDto = objectMapper.convertValue(payload, ProfileDto::class.java)
            val value = profileDto.value
            if ((profileDto.profileType == ProfileType.PROFILE_IMAGE || profileDto.profileType == ProfileType.PROFILE_WALLPAPER) && value != null) {
                if (storageService.exist(value)) {
                    storageService.delete(value)
                    kafkaProducerService.sendMessage(key, profileDto)
                } else {
                    log.info("type: {}, object: {} does not exist", profileDto.profileType, value)
                }
            }
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.info(e.message)
        }
    }
}
