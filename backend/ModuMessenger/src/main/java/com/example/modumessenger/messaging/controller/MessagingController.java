package com.example.modumessenger.messaging.controller;

import com.example.modumessenger.messaging.service.MessagingPublisher;
import com.example.modumessenger.messaging.service.MessagingSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequestMapping("/messaging")
@RestController
@RequiredArgsConstructor
public class MessagingController {
    private final RedisMessageListenerContainer redisMessageListener;
    private final MessagingPublisher redisPublisher;
    private final MessagingSubscriber redisSubscriber;
    private Map<String, ChannelTopic> channels;

    @PostConstruct
    public void init() {
        channels = new HashMap<>();
    }

    @GetMapping("/channel")
    public Set<String> getTopicAll() {
        return channels.keySet();
    }

    @PutMapping("/channel/{name}")
    public void createTopic(@PathVariable String name) {
        ChannelTopic channel = new ChannelTopic(name);
        redisMessageListener.addMessageListener(redisSubscriber, channel);
        channels.put(name, channel);
    }

    @PostMapping("/channel/{name}")
    public void pushMessage(@PathVariable String name, @RequestParam String message) {
        ChannelTopic channel = channels.get(name);
        redisPublisher.publish(channel, message);
    }

    @DeleteMapping("/channel/{name}")
    public void deleteTopic(@PathVariable String name) {
        ChannelTopic channel = channels.get(name);
        redisMessageListener.removeMessageListener(redisSubscriber, channel);
        channels.remove(name);
    }
}