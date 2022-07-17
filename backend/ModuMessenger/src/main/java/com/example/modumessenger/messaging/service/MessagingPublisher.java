package com.example.modumessenger.messaging.service;

import com.example.modumessenger.messaging.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessagingPublisher {
    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(ChannelTopic topic, String message) {
        redisTemplate.convertAndSend(topic.getTopic(), message);
    }

    public void publish(ChannelTopic topic, ChatMessage chatMessage) {
        redisTemplate.convertAndSend(topic.getTopic(), chatMessage);
    }
}
