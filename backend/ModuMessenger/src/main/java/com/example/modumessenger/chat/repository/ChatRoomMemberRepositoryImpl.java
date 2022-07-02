package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.ChatRoomMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.example.modumessenger.chat.entity.QChatRoomMember.*;

@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public ChatRoomMember findByUserIdAndRoomId(String userId, String roomId) {
        return queryFactory
                .selectFrom(chatRoomMember)
                .where(chatRoomMember.member.userId.eq(userId)
                        .and(chatRoomMember.chatRoom.roomId.eq(roomId)))
                .fetchOne();
    }


    @Override
    public ChatRoomMember findByMemberUserId(String userId) {
        return queryFactory
                .selectFrom(chatRoomMember)
                .where(chatRoomMember.member.userId.eq(userId))
                .fetchOne();
    }

    @Override
    public ChatRoomMember findByRoomByRoomId(String roomId) {
        return queryFactory
                .selectFrom(chatRoomMember)
                .where(chatRoomMember.chatRoom.roomId.eq(roomId))
                .fetchOne();
    }
}
