package com.example.modumessenger.chat.service;

import com.example.modumessenger.chat.dto.ChatRoomDto;
import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.chat.repository.ChatRoomRepository;
import com.example.modumessenger.member.dto.MemberDto;
import com.example.modumessenger.member.entity.Member;
import com.example.modumessenger.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ModelMapper modelMapper;

    public List<ChatRoomDto> searchChatRoomByUserId(String userId) {
        Member member = memberRepository.findByUserId(userId);
        List<ChatRoom> chatRoomList = chatRoomRepository.findAllById(member.getId());

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
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId("");
        chatRoom.setRoomName("");
        chatRoom.setRoomImage("");
        chatRoom.setLastChatMsg("");
        chatRoom.setMembers(userId);

        ChatRoom newRoom = chatRoomRepository.save(chatRoom);
        return modelMapper.map(newRoom, ChatRoomDto.class);
    }
}
