package com.example.chatservice.api.pub;

import com.example.chatservice.chat.dto.ChatDto;
import com.example.chatservice.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/** 안드로이드가 게이트웨이를 거쳐 부르는 채팅 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-public/chat")
public class ChatPublicController {

    private final ChatService chatService;

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDto> getChat(@Valid @PathVariable("chatId") String chatId) {
        return ResponseEntity.ok().body(chatService.searchChatById(chatId));
    }

    @GetMapping
    public ResponseEntity<List<ChatDto>> getChatList(@Valid @RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok().body(chatService.searchChatListById(ids));
    }

    @GetMapping("/{roomId}/chats")
    public ResponseEntity<List<ChatDto>> getChatRoomHistory(@Valid @PathVariable("roomId") String roomId) {
        return ResponseEntity.ok().body(chatService.searchChatByRoomId(roomId));
    }

    @GetMapping("/{roomId}/page")
    public ResponseEntity<List<ChatDto>> getChatListPaging(@Valid @PathVariable("roomId") String roomId, Pageable pageable) {
        return ResponseEntity.ok().body(chatService.searchChatByRoomIdPaging(roomId, pageable));
    }

    @GetMapping("/{roomId}/page/{size}")
    public ResponseEntity<List<ChatDto>> getChatListSize(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("size") String size) {
        return ResponseEntity.ok().body(chatService.searchChatByRoomIdSize(roomId, size));
    }

    @GetMapping("/{roomId}/{chatId}/{size}")
    public ResponseEntity<List<ChatDto>> getPrevChatList(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("chatId") String chatId, @Valid @PathVariable("size") String size) {
        return ResponseEntity.ok().body(chatService.searchPrevChatByRoomId(roomId, chatId, size));
    }

    @GetMapping("/{roomId}/images/{size}")
    public ResponseEntity<List<ChatDto>> getImageChatListSize(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("size") String size) {
        return ResponseEntity.ok().body(chatService.searchImageChatByRoomIdSize(roomId, size));
    }

    @GetMapping("/{roomId}/count")
    public ResponseEntity<String> getChatRoomCount(@Valid @PathVariable("roomId") String roomId) {
        return ResponseEntity.ok().body(chatService.searchChatCount(roomId));
    }

    @GetMapping("/{roomId}/{chatId}")
    public ResponseEntity<ChatDto> getChatByChatId(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("chatId") String chatId) {
        return ResponseEntity.ok().body(chatService.searchChatByRoomIdAndChatId(roomId, chatId));
    }

    @GetMapping("/{roomId}/chat")
    public ResponseEntity<List<ChatDto>> getChatByMessage(@Valid @PathVariable("roomId") String roomId, @Valid @RequestParam String message) {
        return ResponseEntity.ok().body(chatService.searchChatByMessage(roomId, message));
    }

    @DeleteMapping("/{roomId}/{chatId}")
    public ResponseEntity<ChatDto> deleteChat(@Valid @PathVariable("roomId") String roomId, @Valid @PathVariable("chatId") String chatId) {
        return ResponseEntity.ok().body(chatService.deleteChat(roomId, chatId));
    }
}
