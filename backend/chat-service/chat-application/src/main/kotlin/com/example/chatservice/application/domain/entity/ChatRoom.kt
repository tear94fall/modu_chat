package com.example.chatservice.application.domain.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "chat_room",
    // 같은 멤버 구성의 방이 동시에 두 개 만들어지는 것을 DB 가 막는다. 테스트 H2(ddl-auto update)도 이 선언으로 같은 제약을 만든다.
    uniqueConstraints = [UniqueConstraint(name = "uk_chat_room_member_key", columnNames = ["member_key"])],
)
class ChatRoom protected constructor() : BaseTimeEntity() {

    @Id
    @Column(name = "chat_room_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(nullable = false)
    var roomId: String? = null
        protected set

    @Column(nullable = false)
    var roomName: String? = null
        protected set

    @Column(nullable = false)
    var roomImage: String? = null
        protected set

    @Column(nullable = false)
    var lastChatMsg: String? = null
        protected set

    @Column(nullable = false)
    var lastChatId: String? = null
        protected set

    @Column(nullable = false)
    var lastChatTime: String? = null
        protected set

    /**
     * 만들 때의 멤버 구성 지문(정렬한 회원 id 를 ',' 로 이은 값의 SHA-256 16진수 64자).
     * 유니크 키가 걸려 있어 같은 구성의 방을 동시에 두 개 만들 수 없다.
     *
     * 멤버가 바뀌면(초대·나가기) [membershipChanged] 로 null 이 된다. 유니크 인덱스는 null 을 세지 않으므로
     * 나중에 구성이 바뀐 방끼리는 절대 부딪히지 않는다. 즉 이 값은 "같은 구성으로 동시에 만들기" 만 막는다.
     * 방을 찾는 일은 예전처럼 멤버 목록 조회(findRoomIdByExactMemberIds)가 한다.
     */
    // columnDefinition 으로 char 를 못 박는다. varchar 로 매핑하면 ddl-auto: validate 가 운영 스키마의 CHAR(64) 와 어긋났다고 기동을 막는다.
    @Column(name = "member_key", columnDefinition = "char(64)", length = MEMBER_KEY_LENGTH, nullable = true)
    var memberKey: String? = null
        protected set

    @OneToMany(mappedBy = "chatRoom", cascade = [CascadeType.ALL])
    var chatRoomMemberList: MutableList<ChatRoomMember> = ArrayList()
        protected set

    @OneToMany(mappedBy = "chatRoom", cascade = [CascadeType.ALL])
    var chat: MutableList<Chat> = ArrayList()
        protected set

    fun addChatting(chat: Chat) {
        this.chat.add(chat)
        this.lastChatId = chat.id.toString()
        this.lastChatMsg = chat.message
    }

    /**
     * 메시지가 지워졌다. 마지막 메시지 id 는 남은 것 중 가장 최근 것으로, 미리보기는 삭제 안내로 바꾼다.
     * [lastRemainingChatId] 는 남은 메시지가 없으면 null 이다(그때 id 는 빈 문자열).
     */
    fun chatDeleted(lastRemainingChatId: Long?) {
        this.lastChatId = lastRemainingChatId?.toString() ?: ""
        this.lastChatMsg = DELETED_CHAT_MESSAGE
    }

    fun updateChatRoom(roomName: String?, roomImage: String?, lastChatMsg: String?, lastChatId: String?, lastChatTime: String?) {
        this.roomName = roomName
        this.roomImage = roomImage
        this.lastChatMsg = lastChatMsg
        this.lastChatId = lastChatId
        this.lastChatTime = lastChatTime
    }

    constructor(roomName: String?) : this() {
        this.roomName = roomName
    }

    @JvmOverloads
    constructor(
        roomId: String?,
        roomName: String?,
        roomImage: String?,
        lastChatMsg: String?,
        lastChatId: String?,
        lastChatTime: String?,
        memberKey: String? = null,
    ) : this() {
        this.roomId = roomId
        this.roomName = roomName
        this.roomImage = roomImage
        this.lastChatMsg = lastChatMsg
        this.lastChatId = lastChatId
        this.lastChatTime = lastChatTime
        this.memberKey = memberKey
    }

    /**
     * 멤버가 바뀌었다(초대·나가기). 만들 때의 멤버 구성 지문은 더 이상 이 방을 설명하지 않으므로 지운다.
     * 유니크 인덱스가 null 을 세지 않으니, 뒤에 같은 구성으로 방을 새로 만들어도 부딪히지 않는다.
     */
    fun membershipChanged() {
        this.memberKey = null
    }

    companion object {
        const val DELETED_CHAT_MESSAGE = "삭제된 메시지 입니다."

        /** `chat_room.member_key` 의 길이(SHA-256 16진수). */
        const val MEMBER_KEY_LENGTH = 64
    }
}
