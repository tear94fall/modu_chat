package com.example.modumessenger.chat.service;

import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;

    public List<ChatRoom> searchChatRoomByUserId(String userId) {
        return chatRoomRepository.findAllByUserId(userId);
    }
}
