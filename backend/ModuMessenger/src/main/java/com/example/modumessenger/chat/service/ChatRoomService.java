package com.example.modumessenger.chat.service;

import com.example.modumessenger.chat.dto.ChatRoomDto;
import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ModelMapper modelMapper;

    public List<ChatRoomDto> searchChatRoomByUserId(String userId) {
        List<ChatRoom> chatRoomList = chatRoomRepository.findAllById(userId);

        return chatRoomList
                .stream()
                .map(c -> modelMapper.map(c, ChatRoomDto.class))
                .collect(Collectors.toList());
    }

    public ChatRoomDto searchChatRoomByRoomId(String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId);
        return modelMapper.map(chatRoom, ChatRoomDto.class);
    }

    public ChatRoomDto createChatRoom(List<String> userId) {
        LocalDateTime TimeNow = LocalDateTime.now();
        String sendTime = TimeNow.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId(UUID.randomUUID().toString());
        chatRoom.setRoomName("새로운 채팅방");
        chatRoom.setRoomImage("");
        chatRoom.setLastChatMsg("");
        chatRoom.setLastChatTime(sendTime);
        chatRoom.setUserIds(userId);

        ChatRoom newRoom = chatRoomRepository.save(chatRoom);
        return modelMapper.map(newRoom, ChatRoomDto.class);
    }

    public ChatRoomDto removeChatRoomMember(String roomId, String userId) {
        ChatRoom findChatRoom = chatRoomRepository.findByRoomId(roomId);
        findChatRoom.getUserIds().remove(userId);
        ChatRoom removeChatRoom = chatRoomRepository.save(findChatRoom);
        return modelMapper.map(removeChatRoom, ChatRoomDto.class);
    }

    public ChatRoomDto updateChatRoom(String roomId, ChatRoomDto chatRoomDto) {
        ChatRoom findChatRoom = chatRoomRepository.findByRoomId(roomId);

        findChatRoom.setRoomName(chatRoomDto.getRoomName());
        findChatRoom.setRoomImage(chatRoomDto.getRoomImage());

        ChatRoom updateChatRoom = chatRoomRepository.save(findChatRoom);
        return modelMapper.map(updateChatRoom, ChatRoomDto.class);
    }
}
