package com.example.chatservice.application.usecase

import com.example.chatservice.application.domain.repository.ChatRoomSort
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.service.ChatQueryService
import com.example.chatservice.application.service.ChatRoomQueryService
import com.example.chatservice.application.usecase.result.AdminChatRoomSummary
import com.example.chatservice.application.usecase.result.ChatResult
import com.example.chatservice.application.usecase.result.ChatRoomView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/** 백오피스의 방·대화 둘러보기. 모두 replica 로 읽는다. */
@Component
class ChatAdminUseCase(
    private val chatRoomQueryService: ChatRoomQueryService,
    private val chatQueryService: ChatQueryService,
    private val memberGateway: MemberGateway,
) {

    fun rooms(sort: ChatRoomSort, pageable: Pageable): Page<AdminChatRoomSummary> =
        chatRoomQueryService.findRoomsForAdmin(sort, pageable)

    /** 방 상세. 멤버의 회원 정보는 member-service 에서 받는다. */
    fun room(roomId: String): ChatRoomView {
        val room = chatRoomQueryService.findRoomForAdmin(roomId)
        return ChatRoomView(room, memberGateway.byIds(room.memberIds))
    }

    fun chats(roomId: String, pageable: Pageable): Page<ChatResult> = chatQueryService.findPageForAdmin(roomId, pageable)
}
