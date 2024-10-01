package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.QChatRoomMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.chatservice.chat.entity.QChat.*;
import static com.example.chatservice.chat.entity.QChatRoom.chatRoom;
import static com.example.chatservice.chat.entity.QChatRoomMember.*;

@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatRoom> findAllQueryDsl() {
        return queryFactory
                .selectFrom(chatRoom)
                .leftJoin(chatRoom.chat, chat)
                .fetchJoin()
                .fetch();
    }

    @Override
    public Long countAll() {
        return queryFactory
                .select(chatRoom.count())
                .from(chatRoom)
                .fetchOne();
    }

    @Override
    public List<ChatRoom> findAllByMemberId(Long memberId) {
        return queryFactory
                .select(chatRoom)
                .leftJoin(chatRoom.chatRoomMemberList, chatRoomMember)
                .where(chatRoomMember.memberId.eq(memberId))
                .fetchJoin()
                .fetch();
    }
}
