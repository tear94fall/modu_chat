package com.example.chatservice.application.service

import com.example.chatservice.application.common.exception.CustomException
import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.config.RoJpaConfig
import com.example.chatservice.application.domain.entity.ChatRoom
import com.example.chatservice.application.domain.repository.ChatRoomSort
import com.example.chatservice.application.domain.repository.ro.ChatRoomRoRepository
import com.example.chatservice.application.usecase.result.AdminChatRoomSummary
import com.example.chatservice.application.usecase.result.ChatRoomResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 백오피스의 방 둘러보기(replica). 복제가 따라오기 전에는 방금 만든 방이 안 보일 수 있다. */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class ChatRoomQueryService(private val chatRoomRepository: ChatRoomRoRepository) {

    /**
     * 백오피스 방 목록. 멤버 수를 세기 위해 같은 트랜잭션 안에서 컬렉션을 초기화한다.
     * 정렬은 [ChatRoomSort] 가 정한다 — 멤버 수만 엔티티 속성이 아니라서 전용 질의로 간다.
     * pageable 에는 쪽 번호와 크기만 있으면 된다. 정렬은 여기서 붙인다.
     */
    fun findRoomsForAdmin(sort: ChatRoomSort, pageable: Pageable): Page<AdminChatRoomSummary> {
        val request = PageRequest.of(pageable.pageNumber, pageable.pageSize, sort.sort())
        val rooms: Page<ChatRoom> = when (sort) {
            ChatRoomSort.MEMBER_COUNT_ASC -> chatRoomRepository.findAllOrderByMemberCountAsc(request)
            ChatRoomSort.MEMBER_COUNT_DESC -> chatRoomRepository.findAllOrderByMemberCountDesc(request)
            else -> chatRoomRepository.findAll(request)
        }
        return rooms.map { AdminChatRoomSummary.of(it) }
    }

    /** 백오피스 방 상세. 없는 방이면 404. */
    fun findRoomForAdmin(roomId: String): ChatRoomResult =
        chatRoomRepository.findByRoomId(roomId)
            .map { ChatRoomResult.of(it) }
            .orElseThrow { CustomException(ErrorCode.CHATROOM_NOT_FOUND_ERROR, roomId) }
}
