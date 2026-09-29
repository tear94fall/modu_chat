package com.example.chatservice.application.domain.repository.ro

import com.example.chatservice.application.config.RoRepository
import com.example.chatservice.application.domain.entity.ChatRoom
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query

/** 백오피스 방 목록·상세(replica). */
interface ChatRoomRoRepository : RoRepository<ChatRoom, Long>, ChatRoomRoCustomRepository {

    fun findByRoomId(roomId: String): Optional<ChatRoom>

    fun findAll(pageable: Pageable): Page<ChatRoom>

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
