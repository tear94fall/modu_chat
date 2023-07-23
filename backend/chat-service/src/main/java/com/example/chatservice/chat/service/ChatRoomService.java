package com.example.chatservice.chat.service;

import com.example.chatservice.chat.dto.ChatRoomDto;
import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatRoomMember;
import com.example.chatservice.chat.repository.ChatRoomMemberRepository;
import com.example.chatservice.chat.repository.ChatRoomRepository;
import com.example.chatservice.common.exception.CustomException;
import com.example.chatservice.common.exception.ErrorCode;
import com.example.chatservice.member.client.MemberFeignClient;
import com.example.chatservice.member.dto.MemberDto;
import com.example.chatservice.member.dto.ChatRoomMemberDto;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberFeignClient memberFeignClient;
    private final ModelMapper modelMapper;

    public List<ChatRoomDto> searchChatRoomByUserId(String userId) {
        MemberDto member = memberFeignClient.getMember(userId);
        List<ChatRoomMember> chatRoomMemberList = chatRoomMemberRepository.findAllByMemberUserId(member.getId());

        List<Long> chatRoomMemberIds = chatRoomMemberList
                .stream()
                .map(ChatRoomMember::getMemberId)
                .collect(Collectors.toList());

        List<MemberDto> members = memberFeignClient.getMembersById(chatRoomMemberIds);

        return chatRoomMemberList.stream()
                .map(chatRoomMember -> new ChatRoomDto(chatRoomMember.getChatRoom(), members))
                .collect(Collectors.toList());
    }

    public ChatRoomDto searchChatRoomByRoomId(String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId));

        List<Long> chatRoomMemberIds = chatRoom.getChatRoomMemberList()
                .stream()
                .map(ChatRoomMember::getMemberId)
                .collect(Collectors.toList());

        List<MemberDto> members = memberFeignClient.getMembersById(chatRoomMemberIds);

        return new ChatRoomDto(chatRoom, members);
    }

    public List<ChatRoomDto> searchOneOnOneChatRoom(String userId, String roomUserId) {
        MemberDto member = memberFeignClient.getMember(userId);
        List<MemberDto> members = memberFeignClient.getMembersByUserId(new ArrayList<>(Arrays.asList(userId, roomUserId)));

        List<ChatRoomMember> chatRoomMemberList = chatRoomMemberRepository.findAllByMemberUserId(member.getId());
        List<ChatRoom> chatRoomList = chatRoomMemberList.stream()
//                .filter(ChatRoomMember::isOneOnOne)
                .map(ChatRoomMember::getChatRoom)
                .filter(chatRoom -> {
                    List<Long> roomMemberId = chatRoom.getChatRoomMemberList().stream()
                            .map(ChatRoomMember::getMemberId)
                            .collect(Collectors.toList());

                    return roomMemberId.size() == 2 && roomMemberId.contains(members.get(0).getId()) && roomMemberId.contains(members.get(1).getId());
                })
                .collect(Collectors.toList());

        return chatRoomList.stream()
                .map(chatRoom -> {
                    return new ChatRoomDto(chatRoom, members);
                })
                .collect(Collectors.toList());
    }

    public ChatRoomDto createChatRoom(List<String> userIds) {
        List<MemberDto> members = memberFeignClient.getMembersByUserId(userIds);
        if(members.size() == 0) {
            throw new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userIds.toString());
        }

        //legacy
        String chatRoomCreateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ChatRoom chatRoom = new ChatRoom(UUID.randomUUID().toString(), "새로운 채팅방", "", "", "", chatRoomCreateTime);

        return addNewChatRoomMember(chatRoom, members);
    }

    public ChatRoomDto exitChatRoomMember(String roomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId));
        List<MemberDto> members = memberFeignClient.getMembersByUserId(new ArrayList<>(Collections.singletonList(userId)));
        if(members.size() == 0) {
            throw new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId);
        }

        ChatRoomDto chatRoomDto = addNewChatRoomMember(chatRoom, members);

        ChatRoomMemberDto memberInviteDto = new ChatRoomMemberDto(chatRoomDto, members);

        List<MemberDto> invited = memberFeignClient.exitChatRoom(memberInviteDto);

        return addNewChatRoomMember(chatRoom, invited);
    }

    public ChatRoomDto updateChatRoom(String roomId, ChatRoomDto chatRoomDto) {
        ChatRoom findChatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId));

        findChatRoom.setRoomName(chatRoomDto.getRoomName());
        findChatRoom.setRoomImage(chatRoomDto.getRoomImage());
        findChatRoom.setLastChatMsg(chatRoomDto.getLastChatMsg());
        findChatRoom.setLastChatId(chatRoomDto.getLastChatId());
        findChatRoom.setLastChatTime(chatRoomDto.getLastChatTime());

        ChatRoom updateChatRoom = chatRoomRepository.save(findChatRoom);
        return modelMapper.map(updateChatRoom, ChatRoomDto.class);
    }

    public ChatRoomDto addMemberChatRoom(String roomId, List<String> userIds) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId));
        List<MemberDto> members = memberFeignClient.getMembersByUserId(userIds);
        if(members.size() == 0) {
            throw new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userIds.toString());
        }

        ChatRoomDto chatRoomDto = addNewChatRoomMember(chatRoom, members);

        ChatRoomMemberDto memberInviteDto = new ChatRoomMemberDto(chatRoomDto, members);

        List<MemberDto> invited = memberFeignClient.inviteChatRoom(memberInviteDto);

        return addNewChatRoomMember(chatRoom, invited);
    }

    private ChatRoomDto addNewChatRoomMember(ChatRoom chatRoom, List<MemberDto> members) {
        List<ChatRoomMember> chatRoomMemberList = members.stream()
                .map(member -> {
                    ChatRoomMember chatRoomMember = new ChatRoomMember(member.getId(), chatRoom);
                    chatRoom.getChatRoomMemberList().add(chatRoomMember);
                    return chatRoomMember;
                })
                .collect(Collectors.toList());

        chatRoomMemberRepository.saveAll(chatRoomMemberList);
        ChatRoom newRoom = chatRoomRepository.save(chatRoom);

        return modelMapper.map(newRoom, ChatRoomDto.class);
    }
}
