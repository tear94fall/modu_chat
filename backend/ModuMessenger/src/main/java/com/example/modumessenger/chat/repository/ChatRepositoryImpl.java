package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

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
    public Long countAll() {
        return queryFactory
                .select(chat.count())
                .from(chat)
                .fetchOne();
    }
}
