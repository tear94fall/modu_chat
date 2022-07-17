package com.example.modumessenger.chat.service;

import com.example.modumessenger.chat.dto.ChatRoomDto;
import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.chat.entity.ChatRoomMember;
import com.example.modumessenger.chat.repository.ChatRoomMemberRepository;
import com.example.modumessenger.chat.repository.ChatRoomRepository;
import com.example.modumessenger.member.entity.Member;
import com.example.modumessenger.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public List<ChatRoomDto> searchOneOnOneChatRoom(String userId, String roomUserId) {
        List<ChatRoomMember> chatRoomMemberList = chatRoomMemberRepository.findAllByMemberUserId(userId);
        List<ChatRoom> chatRoomList = chatRoomMemberList.stream()
//                .filter(ChatRoomMember::isOneOnOne)
                .map(ChatRoomMember::getChatRoom)
                .filter(chatRoom -> {
                    List<String> roomMemberId = chatRoom.getChatRoomMemberList().stream()
                            .map(chatRoomMember -> chatRoomMember.getMember().getUserId())
                            .collect(Collectors.toList());

                    return roomMemberId.size() == 2 && roomMemberId.contains(userId) && roomMemberId.contains(roomUserId);
                })
                .collect(Collectors.toList());

        return chatRoomList.stream()
                .map(ChatRoomDto::new)
                .collect(Collectors.toList());
    }

    public ChatRoomDto createChatRoom(List<String> userId) {
        List<Member> members = memberRepository.findAllByUserIds(userId);

        String chatRoomCreateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ChatRoom chatRoom = new ChatRoom(UUID.randomUUID().toString(), "새로운 채팅방", "", "", "", chatRoomCreateTime);

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
        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByUserIdAndRoomId(userId, roomId);

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
        findChatRoom.setLastChatMsg(chatRoomDto.getLastChatMsg());
        findChatRoom.setLastChatId(chatRoomDto.getLastChatId());
        findChatRoom.setLastChatTime(chatRoomDto.getLastChatTime());

        ChatRoom updateChatRoom = chatRoomRepository.save(findChatRoom);
        return modelMapper.map(updateChatRoom, ChatRoomDto.class);
    }

    public ChatRoomDto addMemberChatRoom(String roomId, List<String> userIds) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId);
        List<Member> members = memberRepository.findAllByUserIds(userIds);

        chatRoom.getChatRoomMemberList().forEach(chatRoomMember -> members.remove(chatRoomMember.getMember()));

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
}
