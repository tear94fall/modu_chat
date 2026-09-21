package com.example.chatservice.chat.service;

import com.example.chatservice.chat.dto.ChatDto;
import com.example.chatservice.chat.dto.ReactionResultDto;
import com.example.chatservice.chat.dto.ReactionSummaryDto;
import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.entity.ChatReaction;
import com.example.chatservice.chat.entity.ReactionEmoji;
import com.example.chatservice.chat.repository.ChatReactionRepository;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.common.exception.CustomException;
import com.example.chatservice.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 메시지 반응. 한 사람은 한 메시지에 이모지 하나만: 같은 이모지를 다시 보내면 취소, 다른 이모지면 교체.
 * 내 메시지에는 남길 수 없다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChatReactionService {

    private final ChatRepository chatRepository;
    private final ChatReactionRepository chatReactionRepository;

    public ReactionResultDto react(String roomId, Long chatId, String userId, String rawEmoji) {
        ReactionEmoji emoji = ReactionEmoji.of(rawEmoji)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REACTION, rawEmoji));
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId));
        if (!roomId.equals(chat.getRoomId())) {
            throw new CustomException(ErrorCode.CHAT_NOT_FOUND_ERROR, chatId);
        }
        if (userId.equals(chat.getSender())) {
            throw new CustomException(ErrorCode.CANNOT_REACT_OWN_CHAT, chatId);
        }

        Optional<ChatReaction> existing = chatReactionRepository.findByChatIdAndUserId(chatId, userId);
        boolean added;
        if (existing.isEmpty()) {
            chatReactionRepository.save(new ChatReaction(chatId, roomId, userId, emoji));
            added = true;
        } else if (existing.get().getEmoji() == emoji) {
            chatReactionRepository.delete(existing.get());
            added = false;
        } else {
            existing.get().change(emoji);
            added = true;
        }
        chatReactionRepository.flush();

        return ReactionResultDto.builder()
                .chatId(chatId)
                .roomId(roomId)
                .authorUserId(chat.getSender())
                .added(added)
                .emoji(added ? emoji.name() : null)
                .reactions(summaryOf(chatId))
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReactionSummaryDto> summaryOf(Long chatId) {
        return ReactionSummaryDto.summarize(chatReactionRepository.findAllByChatIdInOrderByIdAsc(List.of(chatId)));
    }

    /** 이력 한 페이지의 DTO 들에 반응 집계를 채운다. 한 번의 IN 질의로 끝낸다. */
    @Transactional(readOnly = true)
    public List<ChatDto> withReactions(List<ChatDto> chats) {
        if (chats == null || chats.isEmpty()) return chats;
        List<Long> ids = chats.stream().map(ChatDto::getId).filter(id -> id != null).collect(Collectors.toList());
        Map<Long, List<ChatReaction>> byChat = new HashMap<>();
        if (!ids.isEmpty()) {
            for (ChatReaction r : chatReactionRepository.findAllByChatIdInOrderByIdAsc(ids)) {
                byChat.computeIfAbsent(r.getChatId(), k -> new java.util.ArrayList<>()).add(r);
            }
        }
        chats.forEach(c -> c.setReactions(ReactionSummaryDto.summarize(byChat.getOrDefault(c.getId(), Collections.emptyList()))));
        return chats;
    }

    /** 메시지가 지워지면 반응도 같이 지운다. */
    public void deleteAllOf(Long chatId) {
        chatReactionRepository.deleteAllByChatId(chatId);
    }
}
