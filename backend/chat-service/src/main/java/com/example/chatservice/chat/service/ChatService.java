package com.example.chatservice.chat.service;

import com.example.chatservice.chat.dto.ChatDto;
import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ChatRoomRepository;
import com.example.chatservice.common.exception.CustomException;
import com.example.chatservice.common.exception.ErrorCode;
import com.example.chatservice.member.service.BlockedIdsCache;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    /** 1:1 방의 정의: 방 멤버가 정확히 2명. */
    private static final int ONE_ON_ONE_MEMBER_COUNT = 2;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final ModelMapper modelMapper;
    private final BlockedIdsCache blockedIdsCache;
    private final ChatReactionService chatReactionService;

    /**
     * 방 전체 이력. requesterUserId(게이트웨이가 넣는 X-Auth-User-Id)가 있고 1:1 방이면
     * 그 사람이 차단한 발신자의 메시지를 뺀다. 헤더가 없으면(내부 호출) 필터 없음.
     */
    public List<ChatDto> searchChatByRoomId(String roomId, String requesterUserId) {
        List<Chat> chatList = chatRepository.findAllByRoomId(roomId, blockedSendersIn(roomId, requesterUserId));
        return chatReactionService.withReactions(chatList
                .stream()
                .map(c -> modelMapper.map(c, ChatDto.class))
                .collect(Collectors.toList()));
    }

    public List<ChatDto> searchChatByRoomIdSize(String roomId, String size, String requesterUserId) {
        List<Chat> chatList = chatRepository.findByRoomIdSize(roomId, Long.parseLong(size),
                blockedSendersIn(roomId, requesterUserId));
        return chatReactionService.withReactions(chatList
                .stream()
                .map(c -> modelMapper.map(c, ChatDto.class))
                .collect(Collectors.toList()));
    }

    public List<ChatDto> searchPrevChatByRoomId(String roomId, String chatId, String size, String requesterUserId) {
        List<Chat> chatList = chatRepository.findByRoomIdAndChatId(roomId, Long.parseLong(chatId), Long.parseLong(size),
                blockedSendersIn(roomId, requesterUserId));
        return chatReactionService.withReactions(chatList
                .stream()
                .map(c -> modelMapper.map(c, ChatDto.class))
                .collect(Collectors.toList()));
    }

    public List<ChatDto> searchChatByRoomIdPaging(String roomId, Pageable pageable) {
        List<Chat> chatList = chatRepository.findByRoomIdPaging(roomId, pageable);
        return chatReactionService.withReactions(chatList
                .stream()
                .map(c -> modelMapper.map(c, ChatDto.class))
                .collect(Collectors.toList()));
    }

    public List<ChatDto> searchImageChatByRoomIdSize(String roomId, String size, String requesterUserId) {
        List<Chat> chatList = chatRepository.findByImageChatSize(roomId, Long.parseLong(size),
                blockedSendersIn(roomId, requesterUserId));
        return chatReactionService.withReactions(chatList
                .stream()
                .map(c -> modelMapper.map(c, ChatDto.class))
                .collect(Collectors.toList()));
    }

    public List<ChatDto> searchChatByMessage(String roomId, String message) {
        List<Chat> chatList = chatRepository.findByMessage(roomId, message);
        return chatReactionService.withReactions(chatList
                .stream()
                .map(c -> modelMapper.map(c, ChatDto.class))
                .collect(Collectors.toList()));
    }

    public ChatDto searchChatById(String chatId) {
        Long id = Long.parseLong(chatId);
        Chat chat = chatRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId));
        ChatDto dto = modelMapper.map(chat, ChatDto.class);
        dto.setReactions(chatReactionService.summaryOf(id));
        return dto;
    }

    /**
     * id 목록 조회는 방이 섞여 올 수 있다. 방마다 1:1 인지 따지는 일은 질의가 대신한다
     * (방 멤버 수 = 2 인 방의 차단된 발신자만 제외).
     */
    public List<ChatDto> searchChatListById(List<String> chatIdList, String requesterUserId) {
        List<Long> chatIds = chatIdList
                .stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Chat> chatList = chatRepository.findAllByIdIn(chatIds, blockedIdsCache.get(requesterUserId));

        return chatReactionService.withReactions(chatList
                .stream()
                .map(c -> modelMapper.map(c, ChatDto.class))
                .collect(Collectors.toList()));
    }

    public Long saveChat(ChatDto chatDto) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(chatDto.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, chatDto.getRoomId()));

        Chat chat = new Chat(chatDto);
        chat.addChatRoom(chatRoom);
        Chat saveChat = chatRepository.save(chat);

        chatRoom.addChatting(chat);

        return saveChat.getId();
    }

    public ChatDto deleteChat(String roomId, String chatId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId));

        Chat chat = chatRepository.findById(Long.parseLong(chatId))
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId));

        chatRoom.removeChat(chat);

        chatRepository.deleteById(chat.getId());

        return modelMapper.map(chat, ChatDto.class);
    }

    public ChatDto searchChatByRoomIdAndChatId(String chatId, String roomId) {
        Long id = Long.parseLong(chatId);
        Chat chat = chatRepository.findByRoomIdAndChatId(roomId, id);
        return modelMapper.map(chat, ChatDto.class);
    }

    public String searchChatCount(String roomId) {
        Long id = chatRepository.countByRoomId(roomId);
        return id.toString();
    }

    /**
     * 이 요청에서 뺄 발신자들. 단체방이면 빈 집합이라 질의가 그대로 돌아간다.
     * 방을 먼저 보는 이유: 단체방에는 필터가 없으므로 member-service 를 부를 필요도 없다.
     */
    private Set<String> blockedSendersIn(String roomId, String requesterUserId) {
        if (requesterUserId == null || requesterUserId.isBlank()) {
            return Set.of();
        }
        if (!isOneOnOneRoom(roomId)) {
            return Set.of();
        }
        return blockedIdsCache.get(requesterUserId);
    }

    private boolean isOneOnOneRoom(String roomId) {
        return chatRoomRepository.findByRoomId(roomId)
                .map(room -> room.getChatRoomMemberList().size() == ONE_ON_ONE_MEMBER_COUNT)
                .orElse(false);
    }

    /** 백오피스 방 메시지 목록. 최신 순 페이징. */
    @Transactional(readOnly = true)
    public Page<ChatDto> searchChatsForAdmin(String roomId, Pageable pageable) {
        return chatRepository.findByRoomId(roomId, pageable).map(c -> modelMapper.map(c, ChatDto.class));
    }
}
