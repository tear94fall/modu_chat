package com.example.modumessenger.messaging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String msg = (String) redisTemplate.getValueSerializer().deserialize(message.getBody());

            String data = objectMapper.readValue(msg, String.class);
            log.info("[onMessage] channel: {}, message: {}", channel, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
