package com.example.memberservice.kafka.consumer;

import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.service.MemberService;
import com.example.memberservice.profile.dto.ProfileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;
    private final MemberService memberService;

    @KafkaListener(
            topics = "topic-member-rollback",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void receive(ConsumerRecord<String, Object> consumerRecord, Acknowledgment acknowledgment) {
        try {
            String key = consumerRecord.key();
            Object payload = consumerRecord.value();
            log.info("received payload = {}", payload.toString());

            ProfileDto profileDto = objectMapper.convertValue(payload, ProfileDto.class);
            MemberDto memberDto = memberService.rollbackMemberProfile(Long.parseLong(key), profileDto);

            log.info("memberDto = {}", memberDto.toString());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}
