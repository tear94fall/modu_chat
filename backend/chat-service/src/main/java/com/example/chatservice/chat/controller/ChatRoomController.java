package com.example.chatservice.chat.controller;

import com.example.chatservice.chat.dto.ChatRoomDto;
import com.example.chatservice.chat.dto.ChatRoomLastReadChatDto;
import com.example.chatservice.chat.service.ChatRoomService;
import com.example.chatservice.common.exception.CustomException;
import com.example.chatservice.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @GetMapping("/chat/{memberId}/rooms")
    public ResponseEntity<List<ChatRoomDto>> chatRoomList(@Valid @PathVariable("memberId") String memberId) {
        List<ChatRoomDto> chatRoomList = chatRoomService.searchChatRoomByUserId(memberId);
        return ResponseEntity.ok().body(chatRoomList);
    }

    @GetMapping("/chat/{roomId}/room")
    public ResponseEntity<ChatRoomDto> getChatRoomInfo(@Valid @PathVariable("roomId") String roomId) {
        ChatRoomDto chatRoomDto = chatRoomService.searchChatRoomByRoomId(roomId);
        return ResponseEntity.ok().body(chatRoomDto);
    }

    @PostMapping("/chat/chat/room")
    public ResponseEntity<ChatRoomDto> createChatRoom(@Valid @RequestBody List<Long> ids) {
        ChatRoomDto chatRoomDto = chatRoomService.createChatRoom(ids);
        return ResponseEntity.ok().body(chatRoomDto);
    }

    @GetMapping("/chat/{roomId}/{userId}")
    public ResponseEntity<String> checkValidChatRoomMember(@Valid @PathVariable("roomId") String roomId, @PathVariable("userId") String userId) throws CustomException {
        ChatRoomDto chatRoomDto = chatRoomService.searchChatRoomByRoomId(roomId);

        if(chatRoomDto.checkChatRoomMember(userId))
            throw new CustomException(ErrorCode.INVALID_CHAT_ROOM_MEMBER, roomId);

        return ResponseEntity.ok().body(userId);
    }

    @DeleteMapping("/chat/{roomId}/{userId}")
    public ResponseEntity<ChatRoomDto> removeChatRoomMember(@Valid @PathVariable("roomId") String roomId, @PathVariable("userId") String userId) {
        ChatRoomDto chatRoomDto = chatRoomService.exitChatRoomMember(roomId, userId);
        return ResponseEntity.ok().body(chatRoomDto);
    }

    @PostMapping("/chat/{roomId}/room")
    public ResponseEntity<ChatRoomDto> updateChatRoom(@Valid @PathVariable("roomId") String roomId, @RequestBody ChatRoomDto requestChatRoomDto) {
        ChatRoomDto chatRoomDto = chatRoomService.updateChatRoom(roomId, requestChatRoomDto);
        return ResponseEntity.ok().body(chatRoomDto);
    }

    @PostMapping("/chat/{roomId}/member")
    public ResponseEntity<ChatRoomDto> addMemberChatRoom(@Valid @PathVariable String roomId, @RequestBody List<String> userIds) {
        ChatRoomDto chatRoomDto = chatRoomService.addMemberChatRoom(roomId, userIds);
        return ResponseEntity.ok().body(chatRoomDto);
    }

    @PostMapping("/chat/room/{userId}")
    public ResponseEntity<List<ChatRoomDto>> getOneOnOneChatRoom(@PathVariable("userId") String userId, @Valid @RequestBody String roomUserId) {
        List<ChatRoomDto> chatRoomDtoList = chatRoomService.searchOneOnOneChatRoom(userId, roomUserId);
        return ResponseEntity.ok().body(chatRoomDtoList);
    }

    @GetMapping("/chat/unread/{userId}")
    public ResponseEntity<List<ChatRoomLastReadChatDto>> getUnreadChatRoomChat(@PathVariable("userId") String userId) {
        List<ChatRoomLastReadChatDto> chatRoomLastReadChatDtoList = chatRoomService.searchUnreadChatRoom(userId);
        return ResponseEntity.ok().body(chatRoomLastReadChatDtoList);
    }

    @PostMapping("/chat/read/{roomId}/{userId}")
    public ResponseEntity<Void> updateLastReadChat(@PathVariable("roomId") String roomId, @PathVariable("userId") String userId) {
        chatRoomService.updateLastReadChat(roomId, userId);
        return ResponseEntity.noContent().build();
    }
}
