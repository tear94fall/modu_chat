package com.example.modumessenger.message.client;

import com.example.modumessenger.chat.dto.ChatDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "message-api", url = "localhost:8080")
public interface MessageFeignClient {

    @PostMapping("send")
    ChatDto sendMsg(@RequestBody ChatDto chatDto);
}
