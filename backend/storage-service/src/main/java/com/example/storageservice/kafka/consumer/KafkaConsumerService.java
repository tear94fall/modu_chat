package com.example.storageservice.kafka.consumer;

import com.example.storageservice.kafka.dto.ProfileDto;
import com.example.storageservice.kafka.dto.ProfileType;
import com.example.storageservice.kafka.producer.KafkaProducerService;
import com.example.storageservice.service.StorageService;
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

    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    @KafkaListener(
            topics = "topic-storage-rollback",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void receive(ConsumerRecord<String, Object> consumerRecord, Acknowledgment acknowledgment) {
        try {
            String key = consumerRecord.key();
            Object payload = consumerRecord.value();
            log.info("received payload = {}", payload.toString());

            ProfileDto profileDto = objectMapper.convertValue(payload, ProfileDto.class);

            if (profileDto.getProfileType().equals(ProfileType.PROFILE_IMAGE) || profileDto.getProfileType().equals(ProfileType.PROFILE_WALLPAPER)) {
                if (storageService.exist(profileDto.getValue())) {
                    storageService.delete(profileDto.getValue());

                    kafkaProducerService.sendMessage(key, profileDto);
                } else {
                    log.info("type: {}, object: {} does not exist", profileDto.getProfileType(), profileDto.getValue());
                }
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}
