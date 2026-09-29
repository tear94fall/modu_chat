package com.example.memberservice.api.kafka

import com.example.memberservice.api.dto.ProfileDto
import com.example.memberservice.application.usecase.MemberUseCase
import com.example.memberservice.application.usecase.command.RollbackProfileCommand
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

/** profile-service 가 프로필 저장에 실패했을 때 보내는 되돌리기 메시지(topic-member-rollback)를 받는다. */
@Service
class KafkaConsumerService(
    private val objectMapper: ObjectMapper,
    private val memberUseCase: MemberUseCase,
) {

    private val log = LoggerFactory.getLogger(KafkaConsumerService::class.java)

    @KafkaListener(
        topics = ["topic-member-rollback"],
        groupId = "\${spring.kafka.consumer.group-id}",
    )
    fun receive(consumerRecord: ConsumerRecord<String, Any>, acknowledgment: Acknowledgment) {
        try {
            val key = consumerRecord.key()
            val payload = consumerRecord.value()
            log.info("received payload = {}", payload.toString())

            val profileDto = objectMapper.convertValue(payload, ProfileDto::class.java)
            val member = memberUseCase.rollbackMemberProfile(RollbackProfileCommand(key.toLong(), profileDto.profileType))

            log.info("member = {}", member.toString())

            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.info(e.message)
        }
    }
}
