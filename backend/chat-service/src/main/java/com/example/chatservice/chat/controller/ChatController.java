package com.example.chatservice.chat.controller;

import com.example.chatservice.chat.dto.ChatDto;
import com.example.chatservice.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<Long> createChat(@Valid @RequestBody ChatDto chatDto) {
        return ResponseEntity.ok().body(chatService.saveChat(chatDto));
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<ChatDto> getChat(@Valid @PathVariable("chatId") String chatId) {
        ChatDto chatDto = chatService.searchChatById(chatId);
        return ResponseEntity.ok().body(chatDto);
    }

    @GetMapping("/chat")
    public ResponseEntity<List<ChatDto>> getChatList(@Valid @RequestParam("ids") List<String> ids) {
        List<ChatDto> chatDtoList = chatService.searchChatListById(ids);
        return ResponseEntity.ok().body(chatDtoList);
    }

    @GetMapping("/chat/{roomId}/chats")
    public ResponseEntity<List<ChatDto>> getChatRoomHistory(@Valid @PathVariable("roomId") String roomId) {
        List<ChatDto> chatDtoList = chatService.searchChatByRoomId(roomId);
        return ResponseEntity.ok().body(chatDtoList);
    }

    @GetMapping("/chat/{roomId}/page")
    public ResponseEntity<List<ChatDto>> getChatListPaging(@Valid @PathVariable("roomId") String roomId, Pageable pageable) {
        List<ChatDto> chatDtoList = chatService.searchChatByRoomIdPaging(roomId, pageable);
        return ResponseEntity.ok().body(chatDtoList);
    }

    @GetMapping("/chat/{roomId}/page/{size}")
    public ResponseEntity<List<ChatDto>> getChatListSize(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("size") String size) {
        List<ChatDto> chatDtoList = chatService.searchChatByRoomIdSize(roomId, size);
        return ResponseEntity.ok().body(chatDtoList);
    }

    @GetMapping("/chat/{roomId}/{chatId}/{size}")
    public ResponseEntity<List<ChatDto>> getPrevChatList(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("chatId") String chatId, @Valid @PathVariable("size") String size) {
        List<ChatDto> chatDtoList = chatService.searchPrevChatByRoomId(roomId, chatId, size);
        return ResponseEntity.ok().body(chatDtoList);
    }

    @GetMapping("/chat/{roomId}/images/{size}")
    public ResponseEntity<List<ChatDto>> getImageChatListSize(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("size") String size) {
        List<ChatDto> chatDtoList = chatService.searchImageChatByRoomIdSize(roomId, size);
        return ResponseEntity.ok().body(chatDtoList);
    }

    @GetMapping("/chat/{roomId}/count")
    public ResponseEntity<String> getChatRoomCount(@Valid @PathVariable("roomId") String roomId) {
        String count = chatService.searchChatCount(roomId);
        return ResponseEntity.ok().body(count);
    }

    @GetMapping("/chat/{roomId}/{chatId}")
    public ResponseEntity<ChatDto> getChatByChatId(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("chatId") String chatId) {
        ChatDto chatDto = chatService.searchChatByRoomIdAndChatId(roomId, chatId);
        return ResponseEntity.ok().body(chatDto);
    }

    @GetMapping("/chat/{roomId}/chat")
    public ResponseEntity<List<ChatDto>> getChatByMessage(@Valid @PathVariable("roomId") String roomId, @Valid @RequestParam String message) {
        List<ChatDto> chatDtoList = chatService.searchChatByMessage(roomId, message);
        return ResponseEntity.ok().body(chatDtoList);
    }
}
