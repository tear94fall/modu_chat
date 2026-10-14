package com.example.memberservice.application.usecase.command

import com.example.memberservice.application.common.lock.Lockable
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.entity.ProfileType
import com.example.memberservice.application.domain.entity.StaffPermission

/** auth-service 가 구글 ID 토큰을 검증한 결과. 이메일로 회원을 찾거나 만든다. */
data class GoogleAccountCommand(
    val sub: String?,
    val email: String?,
    val name: String?,
    val picture: String?,
) : Lockable {

    /** 같은 계정의 동시 가입을 직렬화하는 락 키 */
    override val key: String
        get() = "google-$email"
}

/** 프로필 수정. 앱 API 는 네 필드를 통째로 덮어쓰고, 어드민 내 정보 수정은 null 인 필드를 기존 값으로 둔다. */
data class UpdateProfileCommand(
    val username: String?,
    val statusMessage: String?,
    val profileImage: String?,
    val wallpaperImage: String?,
)

/** chat-service 가 알리는 채팅방 참여·나가기. */
data class ChatRoomMembersCommand(val chatRoomId: Long, val memberIds: List<Long>)

data class AddMemberProfileCommand(val memberId: Long, val profileId: Long)

/** profile-service 가 저장에 실패해 되돌리라고 알린 프로필(Kafka topic-member-rollback). */
data class RollbackProfileCommand(val memberId: Long, val profileType: ProfileType?)

data class CreateNoticeCommand(val title: String, val content: String, val push: Boolean, val writerUserId: String?)

data class RecordUsageCommand(val userId: String?, val clientId: String?)

data class BulkUsageCommand(val service: ModuService?, val userIds: List<String>?, val usedAt: String?)

data class SetStaffPermissionsCommand(val actorUserId: String, val memberId: Long, val permissions: List<StaffPermission>?)
