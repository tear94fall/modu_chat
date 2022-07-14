package com.example.modumessenger.chat.service;

import com.example.modumessenger.chat.dto.ChatDto;
import com.example.modumessenger.chat.entity.Chat;
import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.chat.repository.ChatRepository;
import com.example.modumessenger.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final ModelMapper modelMapper;

    public List<ChatDto> searchChatByRoomId(String roomId) {
        List<Chat> chatList = chatRepository.findAllByRoomId(roomId);
        return chatList
                .stream()
                .map(c -> modelMapper.map(c, ChatDto.class))
                .collect(Collectors.toList());
    }

    public List<ChatDto> searchChatByMessage(String roomId, String message) {
        List<Chat> chatList = chatRepository.findByMessage(roomId, message);
        return chatList
                .stream()
                .map(c -> modelMapper.map(c, ChatDto.class))
                .collect(Collectors.toList());
    }

    public ChatDto searchChatById(String chatId) {
        Long id = Long.parseLong(chatId);
        Optional<Chat> chat = chatRepository.findById(id);
        return modelMapper.map(chat, ChatDto.class);
    }

    public Long saveChat(ChatDto chatDto) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(chatDto.getRoomId());
        Chat chat = new Chat(chatDto);
        chat.setChatRoom(chatRoom);
        Chat saveChat = chatRepository.save(chat);

        return saveChat.getId();
    }

    public ChatDto searchChatByRoomIdAndChatId(String roomId, String chatId) {
        Long id = Long.parseLong(chatId);
        Chat chat = chatRepository.findByRoomIdAndChatId(roomId, id);
        return modelMapper.map(chat, ChatDto.class);
    }
}
