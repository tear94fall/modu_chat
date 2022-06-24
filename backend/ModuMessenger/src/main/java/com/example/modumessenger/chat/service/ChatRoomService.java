package com.example.modumessenger.chat.service;

import com.example.modumessenger.chat.dto.ChatRoomDto;
import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.chat.entity.ChatRoomMember;
import com.example.modumessenger.chat.repository.ChatRoomMemberRepository;
import com.example.modumessenger.chat.repository.ChatRoomRepository;
import com.example.modumessenger.member.dto.MemberDto;
import com.example.modumessenger.member.entity.Member;
import com.example.modumessenger.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ModelMapper modelMapper;

    public List<ChatRoomDto> searchChatRoomByUserId(String userId) {
        Member member = memberRepository.findByUserId(userId);
        return member.getChatRoomMemberList().stream()
                .map(chatRoomMember -> new ChatRoomDto(chatRoomMember.getChatRoom()))
                .collect(Collectors.toList());
    }

    public ChatRoomDto searchChatRoomByRoomId(String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId);
        return new ChatRoomDto(chatRoom);
    }

    public ChatRoomDto createChatRoom(List<String> userId) {
        List<Member> members = memberRepository.findAllByUserIds(userId);

        ChatRoom chatRoom = new ChatRoom(UUID.randomUUID().toString(), "새로운 채팅방", "", "", "");

        List<ChatRoomMember> chatRoomMemberList = members.stream()
                        .map(member -> {
                            ChatRoomMember chatRoomMember = new ChatRoomMember(member, chatRoom);
                            chatRoom.getChatRoomMemberList().add(chatRoomMember);
                            return chatRoomMember;
                        })
                .collect(Collectors.toList());

        chatRoomMemberRepository.saveAll(chatRoomMemberList);
        ChatRoom newRoom = chatRoomRepository.save(chatRoom);

        return modelMapper.map(newRoom, ChatRoomDto.class);
    }

    public ChatRoomDto removeChatRoomMember(String roomId, String userId) {
        Member member = memberRepository.findByUserId(userId);
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId);
        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByMemberUserId(userId);

        member.getChatRoomMemberList().remove(chatRoomMember);
        chatRoom.getChatRoomMemberList().remove(chatRoomMember);

        chatRoomMemberRepository.delete(chatRoomMember);
        chatRoomRepository.save(chatRoom);
        memberRepository.save(member);

        return new ChatRoomDto(chatRoom);
    }

    public ChatRoomDto updateChatRoom(String roomId, ChatRoomDto chatRoomDto) {
        ChatRoom findChatRoom = chatRoomRepository.findByRoomId(roomId);

        findChatRoom.setRoomName(chatRoomDto.getRoomName());
        findChatRoom.setRoomImage(chatRoomDto.getRoomImage());

        ChatRoom updateChatRoom = chatRoomRepository.save(findChatRoom);
        return modelMapper.map(updateChatRoom, ChatRoomDto.class);
    }
}
