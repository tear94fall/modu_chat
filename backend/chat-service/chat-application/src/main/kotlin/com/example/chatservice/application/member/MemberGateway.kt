package com.example.chatservice.application.member

/**
 * member-service 호출 포트. 구현(Feign)은 chat-api 에 있다.
 * 유스케이스가 DB 트랜잭션 밖에서 부른다 — 서비스(Query/Command)는 이 포트를 쓰지 않는다.
 * 실패는 구현이 삼키지 않고 그대로 던진다. 삼킬지 말지는 부르는 쪽이 정한다.
 */
interface MemberGateway {

    /** userId(모두 계정 subject)로 회원 한 명. */
    fun byUserId(userId: String): MemberInfo

    fun byUserIds(userIds: List<String>): List<MemberInfo>

    /** 회원 id(숫자 PK)들로 조회. */
    fun byIds(ids: List<Long>): List<MemberInfo>

    /** 회원들의 방 목록에 이 방을 더하고, 실제로 반영된 회원을 돌려준다. */
    fun invite(chatRoomId: Long?, members: List<MemberInfo>): List<MemberInfo>

    /** 회원들의 방 목록에서 이 방을 빼고, 실제로 반영된 회원을 돌려준다. */
    fun exit(chatRoomId: Long?, members: List<MemberInfo>): List<MemberInfo>

    /** userId 가 차단한 사람들의 userId. */
    fun blockedIds(userId: String): List<String?>?
}
