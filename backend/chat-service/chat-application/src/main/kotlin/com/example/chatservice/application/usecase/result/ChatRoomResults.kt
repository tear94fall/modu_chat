package com.example.chatservice.application.usecase.result

import com.example.chatservice.application.domain.entity.ChatRoom
import com.example.chatservice.application.member.MemberInfo
import java.time.LocalDateTime

/** 방 하나(DB 에 있는 값만). 멤버의 회원 정보는 유스케이스가 member-service 에서 받아 [ChatRoomView] 로 합친다. */
data class ChatRoomResult(
    val id: Long?,
    val roomId: String?,
    val roomName: String?,
    val roomImage: String?,
    val lastChatMsg: String?,
    val lastChatId: String?,
    val lastChatTime: String?,
    /** 방 멤버의 회원 id(숫자 PK). 멤버 행 순서 그대로다. */
    val memberIds: List<Long>,
    val createdDate: LocalDateTime?,
) {
    companion object {
        fun of(room: ChatRoom) = ChatRoomResult(
            id = room.id,
            roomId = room.roomId,
            roomName = room.roomName,
            roomImage = room.roomImage,
            lastChatMsg = room.lastChatMsg,
            lastChatId = room.lastChatId,
            lastChatTime = room.lastChatTime,
            memberIds = room.chatRoomMemberList.mapNotNull { it.memberId },
            createdDate = room.createdDate,
        )
    }
}

/** 방과 멤버의 회원 정보. 쓰기 응답(방 만들기·초대·나가기·수정)은 기존 앱 응답대로 [members] 가 비어 있다. */
data class ChatRoomView(val room: ChatRoomResult, val members: List<MemberInfo>)

/** 방 만들기 결과. 같은 멤버 구성의 방이 이미 있었으면 [created] 가 false 다. */
data class CreatedChatRoom(val room: ChatRoomResult, val created: Boolean)

/** 백오피스 방 목록의 한 줄. */
data class AdminChatRoomSummary(
    val id: Long?,
    val roomId: String?,
    val roomName: String?,
    val roomImage: String?,
    val memberCount: Int,
    val lastChatMsg: String?,
    val lastChatTime: String?,
    val createdDate: LocalDateTime?,
) {
    companion object {
        fun of(room: ChatRoom) = AdminChatRoomSummary(
            id = room.id,
            roomId = room.roomId,
            roomName = room.roomName,
            roomImage = room.roomImage,
            memberCount = room.chatRoomMemberList.size,
            lastChatMsg = room.lastChatMsg,
            lastChatTime = room.lastChatTime,
            createdDate = room.createdDate,
        )
    }
}

/** 안 읽은 개수를 세기 전의 방 상태. 차단 목록(member-service)을 받은 뒤 [UnreadCount] 로 센다. */
data class UnreadRoom(
    val roomId: String?,
    val lastSendChatId: Long,
    val lastReadChatId: Long,
    val oneOnOne: Boolean,
    /** 방의 마지막 메시지를 보낸 사람(userId). 모르면 null. */
    val lastSender: String?,
)

data class UnreadCount(
    val roomId: String?,
    val lastSendChatId: Long,
    val lastReadChatId: Long,
    val unreadChatCount: Long,
)

/** 방 멤버 한 명의 읽음 커서(회원 id 기준). userId 는 유스케이스가 붙인다. */
data class MemberReadCursor(val memberId: Long?, val lastReadChatId: Long)

data class ReadCursor(val userId: String, val lastReadChatId: Long)
