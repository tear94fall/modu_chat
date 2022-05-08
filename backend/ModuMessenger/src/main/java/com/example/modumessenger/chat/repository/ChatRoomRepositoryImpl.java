package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.ChatRoom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.modumessenger.chat.entity.QChatRoom.chatRoom;

@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatRoom> findAllByUserId(String userId) {
        return queryFactory
                .selectFrom(chatRoom)
                .where(chatRoom.userIds.contains(userId))
                .fetch();
    }
}
