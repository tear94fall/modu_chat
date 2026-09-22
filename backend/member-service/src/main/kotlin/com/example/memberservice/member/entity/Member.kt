package com.example.memberservice.member.entity

import com.example.memberservice.global.entity.BaseTimeEntity
import com.example.memberservice.member.dto.MemberDto
import com.example.memberservice.member.dto.UpdateProfileDto
import com.example.memberservice.profile.dto.ProfileDto
import com.example.memberservice.profile.dto.ProfileType
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Entity
@Table(
    name = "member",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_member_email", columnNames = ["email"]),
        UniqueConstraint(name = "uk_member_user_id", columnNames = ["user_id"]),
    ],
)
class Member : BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @field:NotNull
    @Column(name = "user_id")
    lateinit var userId: String
        protected set

    var auth: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    var role: Role? = null
        protected set

    @field:NotNull
    lateinit var email: String
        protected set

    var username: String? = null
        protected set

    var statusMessage: String? = null
        protected set
    var profileImage: String? = null
        protected set
    var wallpaperImage: String? = null
        protected set

    @ElementCollection(fetch = FetchType.LAZY)
    var profiles: MutableList<Long>? = null
        protected set

    @ElementCollection(fetch = FetchType.LAZY)
    var chatRoomMembers: MutableList<Long>? = null
        protected set

    /** VARCHAR DEFAULT 'ACTIVE' 라 ddl-auto: update 로 컬럼이 붙어도 기존 행은 ACTIVE 다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(16) DEFAULT 'ACTIVE'")
    var status: MemberStatus = MemberStatus.ACTIVE
        protected set

    @Column(name = "withdrawn_date")
    var withdrawnDate: LocalDateTime? = null
        protected set

    val isWithdrawn: Boolean
        get() = status == MemberStatus.WITHDRAWN

    /**
     * 탈퇴. 행은 남기되 개인정보는 비운다. userId 는 채팅 기록과의 연결이라 그대로 두고,
     * 이메일은 유일 제약(NOT NULL)이 있어 자리표시자로 바꿔 같은 구글 계정이 다시 가입할 수 있게 한다.
     */
    fun withdraw() {
        this.status = MemberStatus.WITHDRAWN
        this.withdrawnDate = LocalDateTime.now()
        this.email = "withdrawn:$userId"
        this.username = WITHDRAWN_USERNAME
        this.statusMessage = ""
        this.profileImage = ""
        this.wallpaperImage = ""
        this.profiles?.clear()
        this.chatRoomMembers?.clear()
    }

    /** 탈퇴했던 사람이 같은 구글 계정으로 다시 로그인하면 같은 행을 되살린다(채팅 기록의 userId 가 그대로 이어진다). */
    fun reactivate(email: String, username: String?) {
        this.status = MemberStatus.ACTIVE
        this.withdrawnDate = null
        this.email = email
        this.username = username
        this.statusMessage = ""
        this.profileImage = ""
        this.wallpaperImage = ""
        this.role = Role.ROLE_MEMBER
    }

    fun addProfile(id: Long) {
        val list = this.profiles ?: mutableListOf<Long>().also { this.profiles = it }
        list.add(id)
    }

    fun addChatRoom(id: Long) {
        val list = this.chatRoomMembers ?: mutableListOf<Long>().also { this.chatRoomMembers = it }
        list.add(id)
    }

    fun delChatRoom(id: Long) {
        this.chatRoomMembers?.remove(id)
    }

    override fun toString(): String =
        "$userId, $username,$email,$auth,$statusMessage,$profileImage"

    fun updateMemberInfo(profileDto: ProfileDto) {
        when (profileDto.profileType) {
            ProfileType.PROFILE_STATUS_MESSAGE -> this.statusMessage = profileDto.value
            ProfileType.PROFILE_IMAGE -> this.profileImage = profileDto.value
            ProfileType.PROFILE_WALLPAPER -> this.wallpaperImage = profileDto.value
            null -> {}
        }
    }

    fun updateProfile(updateProfileDto: UpdateProfileDto) {
        this.username = updateProfileDto.username
        this.statusMessage = updateProfileDto.statusMessage
        this.profileImage = updateProfileDto.profileImage
        this.wallpaperImage = updateProfileDto.wallpaperImage
    }

    /** 자바의 Lombok @Builder 자리. 이름 있는 인자로 필요한 값만 넘긴다. */
    constructor(
        userId: String? = null,
        auth: String? = null,
        role: Role? = null,
        email: String? = null,
        username: String? = null,
        statusMessage: String? = null,
        profileImage: String? = null,
        wallpaperImage: String? = null,
        profiles: MutableList<Long>? = null,
        chatRoomMembers: MutableList<Long>? = null,
    ) : super() {
        userId?.let { this.userId = it }
        this.auth = auth
        this.role = role
        email?.let { this.email = it }
        this.username = username
        this.statusMessage = statusMessage
        this.profileImage = profileImage
        this.wallpaperImage = wallpaperImage
        this.profiles = profiles
        this.chatRoomMembers = chatRoomMembers
    }

    constructor(memberDto: MemberDto) : this(
        userId = memberDto.userId,
        auth = memberDto.auth,
        role = memberDto.role,
        email = memberDto.email,
        username = memberDto.username,
        statusMessage = memberDto.statusMessage,
        profileImage = memberDto.profileImage,
        wallpaperImage = memberDto.wallpaperImage,
        profiles = mutableListOf(),
        chatRoomMembers = mutableListOf(),
    )

    companion object {
        /** 탈퇴한 회원의 표시 이름. 남은 사람들의 대화·방 멤버 목록에서 이렇게 보인다. */
        const val WITHDRAWN_USERNAME = "탈퇴한 회원"

        @JvmStatic
        fun createMember(memberDto: MemberDto): Member = Member(memberDto)
    }
}
