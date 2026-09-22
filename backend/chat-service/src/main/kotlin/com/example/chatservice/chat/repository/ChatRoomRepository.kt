package com.example.chatservice.chat.repository

import com.example.chatservice.chat.entity.ChatRoom
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomRepository : JpaRepository<ChatRoom, Long>, ChatRoomCustomRepository {

    fun findAllByRoomId(roomId: String): List<ChatRoom>

    fun findByRoomName(roomName: String): ChatRoom?

    fun findByRoomId(roomId: String): Optional<ChatRoom>

    /**
     * 멤버 수 오름차순. 멤버 수는 컬렉션 크기라 Sort 로 못 담아 JPQL 의 size() 로 정렬한다.
     * countQuery 를 따로 주는 이유는 기본 count 질의가 order by 절까지 흉내 내면서 깨질 수 있어서다.
     * 넘기는 Pageable 의 Sort 는 비어 있어야 한다. 값이 있으면 Spring Data 가 이 order by 뒤에 덧붙인다.
     */
    @Query(
        value = "select r from ChatRoom r order by size(r.chatRoomMemberList) asc, r.id asc",
        countQuery = "select count(r) from ChatRoom r",
    )
    fun findAllOrderByMemberCountAsc(pageable: Pageable): Page<ChatRoom>

    /** [findAllOrderByMemberCountAsc] 의 내림차순 짝. */
    @Query(
        value = "select r from ChatRoom r order by size(r.chatRoomMemberList) desc, r.id desc",
        countQuery = "select count(r) from ChatRoom r",
    )
    fun findAllOrderByMemberCountDesc(pageable: Pageable): Page<ChatRoom>
}
