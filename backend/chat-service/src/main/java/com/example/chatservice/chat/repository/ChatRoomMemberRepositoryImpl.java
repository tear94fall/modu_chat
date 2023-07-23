package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoomMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.chatservice.chat.entity.QChatRoomMember.*;

@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatRoomMember> findAllByMemberUserId(Long memberId) {
        return queryFactory
                .selectFrom(chatRoomMember)
                .where(chatRoomMember.memberId.eq(memberId))
                .fetch();
    }
}
