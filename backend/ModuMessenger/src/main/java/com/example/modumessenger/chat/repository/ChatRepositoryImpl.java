package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import com.example.modumessenger.chat.entity.QChat;
import com.example.modumessenger.chat.entity.QChatRoom;
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
}
