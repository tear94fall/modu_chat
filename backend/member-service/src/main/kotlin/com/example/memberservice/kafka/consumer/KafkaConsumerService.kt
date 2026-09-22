package com.example.memberservice.kafka.consumer

import com.example.memberservice.member.service.MemberService
import com.example.memberservice.profile.dto.ProfileDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
class KafkaConsumerService(
    private val objectMapper: ObjectMapper,
    private val memberService: MemberService,
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
            val memberDto = memberService.rollbackMemberProfile(key.toLong(), profileDto)

            log.info("memberDto = {}", memberDto.toString())

            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.info(e.message)
        }
    }
}
