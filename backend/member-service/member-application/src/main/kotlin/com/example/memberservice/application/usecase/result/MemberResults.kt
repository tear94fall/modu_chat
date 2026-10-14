package com.example.memberservice.application.usecase.result

import com.example.memberservice.application.domain.entity.FriendStatus
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.domain.entity.MemberServiceUsage
import com.example.memberservice.application.domain.entity.MemberStatus
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.domain.entity.StaffPermission
import com.example.memberservice.application.domain.repository.query.FriendCounts
import com.example.memberservice.application.port.ProfileInfo
import java.time.LocalDateTime

/** 회원 한 명. 트랜잭션 안에서 엔티티를 이 값으로 바꿔 내보낸다. */
data class MemberResult(
    val id: Long?,
    val userId: String?,
    val auth: String?,
    val role: Role?,
    val email: String?,
    val username: String?,
    val statusMessage: String?,
    val profileImage: String?,
    val wallpaperImage: String?,
) {
    companion object {
        fun from(member: Member) = MemberResult(
            id = member.id,
            userId = member.userId,
            auth = member.auth,
            role = member.role,
            email = member.email,
            username = member.username,
            statusMessage = member.statusMessage,
            profileImage = member.profileImage,
            wallpaperImage = member.wallpaperImage,
        )
    }
}

/** 회원 + profile-service 의 프로필 이력. profile-service 가 본문 없이 답하면 profiles 는 null. */
data class MemberProfileResult(val member: MemberResult, val profiles: List<ProfileInfo>?)

/**
 * 구글 계정으로 찾거나 만든 회원. [fresh] 는 방금 만들었거나(가입) 탈퇴 상태에서 되살린 회원 — 이때만 구글 사진을 올린다.
 * [revived] 는 되살린 회원(사진 단계가 실패하면 다시 탈퇴 상태로 돌린다).
 */
data class GoogleMemberResult(val member: MemberResult, val fresh: Boolean, val revived: Boolean = false)

/** 탈퇴할 회원. 다른 서비스 정리에 필요한 값만 담는다. */
data class WithdrawalTarget(val memberId: Long, val userId: String, val profileImage: String?, val wallpaperImage: String?)

/** 내 친구 한 명: 친구 회원 + 내가 정한 이름·즐겨찾기·상태. */
data class FriendResult(
    val member: MemberResult,
    /** 내가 정한 친구 이름. 비어 있으면 클라이언트는 username 을 쓴다. */
    val friendName: String?,
    val favorite: Boolean,
    val status: FriendStatus,
) {
    companion object {
        fun from(memberFriend: MemberFriend) = FriendResult(
            MemberResult.from(memberFriend.friend), memberFriend.friendName, memberFriend.favorite, memberFriend.status,
        )
    }
}

/** 백오피스 회원 목록에 노출할 요약 정보. */
data class AdminMemberSummaryResult(
    val id: Long?,
    val userId: String?,
    val username: String?,
    val profileImage: String?,
    val email: String?,
    val role: Role?,
    val createdDate: LocalDateTime?,
    /** 회원 상세의 친구 목록에서만 채운다(그 회원이 정한 친구 이름). 회원 검색 결과에서는 null. */
    val friendName: String?,
    /** 직원 권한(SUPER, ADMIN, SYSTEM, INTERNAL). 비어 있으면 직원이 아니다. */
    val staffPermissions: List<StaffPermission> = emptyList(),
    /** 이용 서비스(CHAT, COMMERCE 순). 회원 목록에서만 채운다. */
    val services: List<ModuService> = emptyList(),
    val status: MemberStatus = MemberStatus.ACTIVE,
) {
    companion object {
        fun from(member: Member) = AdminMemberSummaryResult(
            member.id, member.userId, member.username, member.profileImage, member.email, member.role,
            member.createdDate, null, status = member.status,
        )

        fun from(memberFriend: MemberFriend): AdminMemberSummaryResult {
            val friend = memberFriend.friend
            return AdminMemberSummaryResult(
                friend.id, friend.userId, friend.username, friend.profileImage, friend.email, friend.role,
                friend.createdDate, memberFriend.friendName, status = friend.status,
            )
        }
    }
}

/** 서비스별 처음·마지막 이용 시각(UTC, 시간대 없음). */
data class ServiceUsageResult(val service: ModuService, val firstUsedAt: LocalDateTime, val lastUsedAt: LocalDateTime) {
    companion object {
        fun from(usage: MemberServiceUsage) = ServiceUsageResult(usage.service, usage.firstUsedAt, usage.lastUsedAt)
    }
}

data class AdminMemberDetailResult(
    val member: MemberResult,
    val friendCount: Int,
    val createdDate: LocalDateTime?,
    val friends: List<AdminMemberSummaryResult>,
    val staffPermissions: List<StaffPermission>,
    val services: List<ServiceUsageResult>,
    val status: MemberStatus,
)

/** 백오피스 회원 상세 친구 탭의 한 줄. */
data class AdminFriendResult(
    val id: Long?,
    val userId: String?,
    val username: String?,
    val profileImage: String?,
    val email: String?,
    val role: Role?,
    val createdDate: LocalDateTime?,
    val friendName: String?,
    val staffPermissions: List<StaffPermission>,
    val services: List<ModuService>,
    /** 친구 회원의 상태(ACTIVE | WITHDRAWN). */
    val status: MemberStatus,
    val favorite: Boolean,
    /** 이 회원이 그 친구에게 매긴 상태(NORMAL | HIDDEN | BLOCKED). */
    val friendStatus: FriendStatus,
) {
    companion object {
        fun from(memberFriend: MemberFriend, staffPermissions: List<StaffPermission>, services: List<ModuService>): AdminFriendResult {
            val friend = memberFriend.friend
            return AdminFriendResult(
                friend.id, friend.userId, friend.username, friend.profileImage, friend.email, friend.role,
                friend.createdDate, memberFriend.friendName, staffPermissions, services, friend.status,
                memberFriend.favorite, memberFriend.status,
            )
        }
    }
}

/** 백오피스 친구 탭 응답. counts 는 필터와 상관없이 이 회원의 모든 친구 행 기준이다. */
data class AdminFriendPageResult(
    val content: List<AdminFriendResult>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val counts: FriendCounts,
)
