package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoomMember;
import com.example.chatservice.chat.entity.QChatRoomMember;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.chatservice.chat.entity.QChatRoomMember.*;

@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatRoomMember> findAllByMemberId(Long memberId) {
        return queryFactory
                .selectFrom(chatRoomMember)
                .where(chatRoomMember.memberId.eq(memberId))
                .fetch();
    }

    /**
     * DB 에서 집계로 걸러 방 id 하나만 받는다. 한 멤버가 속한 방으로 범위를 좁힌 뒤,
     * 멤버 수와 요청 집합에 포함된 멤버 수가 모두 요청 집합 크기와 같은 방만 남긴다.
     * 중복 등록된 멤버 행이 있어도 countDistinct 라서 영향을 받지 않는다.
     */
    @Override
    public Optional<Long> findRoomIdByExactMemberIds(Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Optional.empty();
        }

        Long anyMemberId = memberIds.iterator().next();
        long expectedSize = memberIds.size();
        QChatRoomMember roomOfMember = new QChatRoomMember("roomOfMember");

        NumberExpression<Long> memberIdIfWanted = new CaseBuilder()
                .when(chatRoomMember.memberId.in(memberIds)).then(chatRoomMember.memberId)
                .otherwise((Long) null);

        Long roomId = queryFactory
                .select(chatRoomMember.chatRoom.id)
                .from(chatRoomMember)
                .where(chatRoomMember.chatRoom.id.in(
                        JPAExpressions.select(roomOfMember.chatRoom.id)
                                .from(roomOfMember)
                                .where(roomOfMember.memberId.eq(anyMemberId))))
                .groupBy(chatRoomMember.chatRoom.id)
                .having(chatRoomMember.memberId.countDistinct().eq(expectedSize),
                        memberIdIfWanted.countDistinct().eq(expectedSize))
                .fetchFirst();

        return Optional.ofNullable(roomId);
    }
}
