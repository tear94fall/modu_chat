package com.example.chatstoreservice.chat.service;

import com.example.chatstoreservice.chat.entity.Chat;
import com.example.chatstoreservice.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    @Transactional
    public void createChat(Chat chat) {
        chatRepository.save(chat);
    }

    @Transactional
    public void updateChat(Chat chat) {
        Chat findChat = chatRepository.findByChatId(chat.getChatId())
                .orElseThrow(IllegalArgumentException::new);

        findChat.updateChatMessage(chat.getMessage());
    }

    @Transactional
    public void deleteChat(Chat chat) {
        Chat findChat = chatRepository.findByChatId(chat.getChatId())
                .orElseThrow(IllegalArgumentException::new);

        chatRepository.delete(findChat);
    }
}
