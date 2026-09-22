package com.example.memberservice.member.entity

import com.example.memberservice.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * 내가 추가한 친구 한 명과 내가 그 친구에게 붙인 이름. 단방향이라 A→B 와 B→A 는 별개 행이다.
 * 테이블 이름을 friend 로 하지 않는 이유: 예전 죽은 엔티티가 같은 이름의 테이블을 만들어 두었다.
 */
@Entity
@Table(
    name = "member_friend",
    uniqueConstraints = [UniqueConstraint(name = "uk_member_friend", columnNames = ["member_id", "friend_member_id"])],
)
class MemberFriend protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_friend_id")
    var id: Long? = null
        protected set

    /** 나 */
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    lateinit var member: Member
        protected set

    /** 친구 */
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "friend_member_id", nullable = false)
    lateinit var friend: Member
        protected set

    /** 내가 정한 친구 이름. 비어 있으면 화면은 친구의 현재 username 을 쓴다. */
    @Column(name = "friend_name", nullable = false, length = NAME_MAX_LENGTH)
    var friendName: String = ""
        protected set

    /** 즐겨찾기. 차단하면 꺼진다. TINYINT(1) DEFAULT 0 이라 ddl-auto: update 로 컬럼이 붙어도 기존 행은 0 이다. */
    @Column(name = "favorite", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    var favorite: Boolean = false
        protected set

    /** 숨김·차단 상태. VARCHAR(16) DEFAULT 'NORMAL' 이라 기존 행은 NORMAL 이 된다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(16) DEFAULT 'NORMAL'")
    var status: FriendStatus = FriendStatus.NORMAL
        protected set

    private constructor(member: Member, friend: Member, friendName: String) : this() {
        this.member = member
        this.friend = friend
        this.friendName = friendName
    }

    fun rename(friendName: String?) {
        this.friendName = friendName ?: ""
    }

    /**
     * 즐겨찾기 켜기/끄기. 차단한 친구에게는 즐겨찾기를 쓸 수 없다(400).
     * 전역 예외 처리기가 없어서 CustomException 은 500 이 되므로 FriendSort 처럼 ResponseStatusException 을 쓴다.
     */
    fun updateFavorite(favorite: Boolean) {
        if (status == FriendStatus.BLOCKED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "차단한 친구는 즐겨찾기할 수 없습니다.")
        }
        this.favorite = favorite
    }

    fun hide() {
        this.status = FriendStatus.HIDDEN
    }

    fun unhide() {
        this.status = FriendStatus.NORMAL
    }

    /** 차단하면 즐겨찾기도 함께 꺼진다. */
    fun block() {
        this.status = FriendStatus.BLOCKED
        this.favorite = false
    }

    fun unblock() {
        this.status = FriendStatus.NORMAL
    }

    companion object {
        const val NAME_MAX_LENGTH = 255

        /** 친구 추가. 별칭의 초기값은 상대가 정한 현재 이름이다. */
        @JvmStatic
        fun of(member: Member, friend: Member): MemberFriend = MemberFriend(member, friend, friend.username ?: "")
    }
}
