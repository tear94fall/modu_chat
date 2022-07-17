package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.modumessenger.chat.entity.QChat.chat;
import static com.example.modumessenger.chat.entity.QChatRoom.chatRoom;

@RequiredArgsConstructor
public class ChatRepositoryImpl implements ChatCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Chat> findAllQueryDsl() {
        return queryFactory
                .selectFrom(chat)
                .leftJoin(chat.chatRoom, chatRoom)
                .fetchJoin()
                .fetch();
    }

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
    public List<Chat> findByRoomIdAndChatId(String roomId, Long chatId, Long size) {
        return queryFactory
                .selectFrom(chat)
                .where(chat.roomId.eq(roomId).and(chat.id.lt(chatId)))
                .limit(size)
                .orderBy(chat.chatTime.desc())
                .fetch();
    }
}
