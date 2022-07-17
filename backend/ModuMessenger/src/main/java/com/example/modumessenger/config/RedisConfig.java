package com.example.modumessenger.config;

import com.example.modumessenger.common.Properties.RedisProperties;
import com.example.modumessenger.common.handler.WebSocketHandler;
import com.example.modumessenger.messaging.service.MessagingSubscriber;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {
    private static final String CHAT_MESSAGING_TOPIC_NAME = "modu-chat";

    @Bean
    public RedisMessageListenerContainer redisMessageListener(RedisConnectionFactory connectionFactory, WebSocketHandler webSocketHandler) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(webSocketHandler, createChatMessagingTopic());

        return container;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));

        return redisTemplate;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisProperties.getHost());
        redisStandaloneConfiguration.setPort(redisProperties.getPort());
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    private Topic createChatMessagingTopic() {
        return new ChannelTopic(CHAT_MESSAGING_TOPIC_NAME);
    }
}