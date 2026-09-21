package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatReactionRepository extends JpaRepository<ChatReaction, Long> {

    Optional<ChatReaction> findByChatIdAndUserId(Long chatId, String userId);

    /** 이력 한 페이지의 반응을 한 번에 읽는다. 정렬은 남긴 순서(id). */
    List<ChatReaction> findAllByChatIdInOrderByIdAsc(Collection<Long> chatIds);

    void deleteAllByChatId(Long chatId);
}
