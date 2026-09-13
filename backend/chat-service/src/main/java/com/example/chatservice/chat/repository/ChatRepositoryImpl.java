package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.entity.ChatType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

import static com.example.chatservice.chat.entity.QChat.*;
import static com.example.chatservice.chat.entity.QChatRoomMember.chatRoomMember;

@RequiredArgsConstructor
public class ChatRepositoryImpl implements ChatCustomRepository {

    private static final long ONE_ON_ONE_MEMBER_COUNT = 2L;

    private final JPAQueryFactory queryFactory;

    @Override
    public Chat findByRoomIdAndChatId(String roomId, Long chatId) {
        return queryFactory
                .selectFrom(chat)
                .where(chat.roomId.eq(roomId).and(chat.id.eq(chatId)))
                .fetchOne();
    }

    @Override
    public List<Chat> findByMessage(String roomId, String message) {
        return queryFactory
                .selectFrom(chat)
                .where(chat.roomId.eq(roomId).and(chat.message.contains(message)))
                .fetch();
    }

    @Override
    public List<Chat> findByRoomIdPaging(String roomId, Pageable pageable) {
        return queryFactory
                .selectFrom(chat)
                .where(chat.roomId.eq(roomId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(chat.chatTime.desc())
                .fetch();
    }

    @Override
    public List<Chat> findByRoomIdSize(String roomId, Long size) {
        return findByRoomIdSize(roomId, size, List.of());
    }

    @Override
    public List<Chat> findByRoomIdAndChatId(String roomId, Long chatId, Long size) {
        return findByRoomIdAndChatId(roomId, chatId, size, List.of());
    }

    @Override
    public List<Chat> findByImageChatSize(String roomId, Long size) {
        return findByImageChatSize(roomId, size, List.of());
    }

    @Override
    public List<Chat> findAllByRoomId(String roomId, Collection<String> blockedSenders) {
        return queryFactory
                .selectFrom(chat)
                .where(chat.roomId.eq(roomId), notFrom(blockedSenders))
                .fetch();
    }

    @Override
    public List<Chat> findByRoomIdSize(String roomId, Long size, Collection<String> blockedSenders) {
        return queryFactory
                .selectFrom(chat)
                .where(chat.roomId.eq(roomId), notFrom(blockedSenders))
                .limit(size)
                .orderBy(chat.chatTime.desc())
                .orderBy(chat.id.asc())
                .fetch();
    }

    @Override
    public List<Chat> findByRoomIdAndChatId(String roomId, Long chatId, Long size, Collection<String> blockedSenders) {
        return queryFactory
                .selectFrom(chat)
                .where(chat.roomId.eq(roomId).and(chat.id.lt(chatId)), notFrom(blockedSenders))
                .limit(size)
                .orderBy(chat.chatTime.desc())
                .fetch();
    }

    @Override
    public List<Chat> findByImageChatSize(String roomId, Long size, Collection<String> blockedSenders) {
        return queryFactory
                .selectFrom(chat)
                .where(chat.roomId.eq(roomId).and(chat.chatType.eq(ChatType.CHAT_TYPE_IMAGE)), notFrom(blockedSenders))
                .limit(size)
                .orderBy(chat.chatTime.desc())
                .fetch();
    }

    @Override
    public List<Chat> findAllByIdIn(List<Long> ids, Collection<String> blockedSenders) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .selectFrom(chat)
                .where(chat.id.in(ids), notFromInOneOnOneRoom(blockedSenders))
                .fetch();
    }

    @Override
    public Long countByRoomIdAndIdBetween(String roomId, Long startId, Long endId, Collection<String> blockedSenders) {
        Long count = queryFactory
                .select(chat.count())
                .from(chat)
                .where(chat.roomId.eq(roomId).and(chat.id.between(startId, endId)), notFrom(blockedSenders))
                .fetchOne();
        return count == null ? 0L : count;
    }

    /**
     * 차단한 발신자 제외 조건. 비어 있으면 null 을 줘서 where 에서 사라진다(= 기존 질의).
     * sender 가 null 인 행이 NOT IN 의 3값 논리에 걸려 통째로 빠지지 않게 isNull 을 함께 본다.
     */
    private BooleanExpression notFrom(Collection<String> blockedSenders) {
        if (blockedSenders == null || blockedSenders.isEmpty()) {
            return null;
        }
        return chat.sender.isNull().or(chat.sender.notIn(blockedSenders));
    }

    /**
     * 방이 섞여 들어오는 질의용. "1:1 방이면서 차단한 사람이 보낸" 메시지만 뺀다.
     * 방 멤버 수는 상관 서브쿼리로 센다(멤버가 정확히 2명 = 1:1).
     */
    private BooleanExpression notFromInOneOnOneRoom(Collection<String> blockedSenders) {
        if (blockedSenders == null || blockedSenders.isEmpty()) {
            return null;
        }

        BooleanExpression oneOnOneRoom = JPAExpressions
                .select(chatRoomMember.count())
                .from(chatRoomMember)
                .where(chatRoomMember.chatRoom.id.eq(chat.chatRoom.id))
                .eq(ONE_ON_ONE_MEMBER_COUNT);

        return chat.sender.isNull()
                .or(chat.sender.notIn(blockedSenders))
                .or(oneOnOneRoom.not());
    }
}
