package com.example.memberservice.application.usecase

import com.example.memberservice.application.domain.entity.ProfileType
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.port.ProfilePort
import com.example.memberservice.application.service.MemberCommandService
import com.example.memberservice.application.service.MemberQueryService
import com.example.memberservice.application.usecase.command.AddMemberProfileCommand
import com.example.memberservice.application.usecase.command.ChatRoomMembersCommand
import com.example.memberservice.application.usecase.command.RollbackProfileCommand
import com.example.memberservice.application.usecase.command.UpdateProfileCommand
import com.example.memberservice.application.usecase.result.MemberProfileResult
import com.example.memberservice.application.usecase.result.MemberResult
import org.springframework.stereotype.Component

/**
 * 회원 조회·프로필 유스케이스. 앱(공개)과 다른 서비스(내부)가 같이 쓴다.
 * 프로필 이력(profile-service)은 DB 트랜잭션이 끝난 뒤에 읽어 붙인다.
 */
@Component
class MemberUseCase(
    private val memberQueryService: MemberQueryService,
    private val memberCommandService: MemberCommandService,
    private val profilePort: ProfilePort,
) {

    /** 앱이 로그인 직후(첫 로그인이면 가입 직후) 자기 정보를 읽는다 — master. */
    fun getMemberByEmailWithProfiles(email: String): MemberProfileResult =
        withProfiles(memberCommandService.getMemberByEmail(email))

    /** 친구·대화 상대의 프로필 보기 — 둘러보기라 레플리카. */
    fun getMemberByIdWithProfiles(id: Long): MemberProfileResult =
        withProfiles(memberQueryService.getMemberById(id))

    /** 앱의 내 프로필 수정. 고친 회원을 프로필 이력과 함께 돌려준다. */
    fun updateProfile(userId: String, command: UpdateProfileCommand): MemberProfileResult =
        withProfiles(memberCommandService.updateMemberProfile(userId, command))

    /** auth-service·chat-service 가 가입·로그인 직후에 읽는다 — master. */
    fun getMemberByUserId(userId: String): MemberResult = memberCommandService.getUserById(userId)

    /** auth-service 가 로그인 때 읽는다 — master. */
    fun getMemberByEmail(email: String): MemberResult = memberCommandService.getMemberByEmail(email)

    fun getRole(userId: String): Role? = memberCommandService.getUserById(userId).role

    fun findFriend(email: String): List<MemberResult> = memberQueryService.findFriend(email)

    fun findMembers(userIds: List<String>): List<MemberResult> = memberQueryService.findMembers(userIds)

    fun findMembersById(ids: List<Long>): List<MemberResult> = memberQueryService.findMembersById(ids)

    fun inviteMembers(command: ChatRoomMembersCommand): List<MemberResult> = memberCommandService.inviteMembers(command)

    fun exitMembers(command: ChatRoomMembersCommand): List<MemberResult> = memberCommandService.exitMembers(command)

    fun addMemberProfile(command: AddMemberProfileCommand): Long = memberCommandService.addMemberProfile(command)

    /**
     * profile-service 가 프로필 저장에 실패해 되돌리라고 알렸을 때(Kafka). 회원의 사진·배경을 이력의 마지막 값으로 돌린다.
     *
     * 예전 코드 그대로 옮겼다: 이력이 "비어 있을 때"만 마지막 값을 꺼내려 하므로(`isEmpty()`), 이력이 있으면 아무것도 바꾸지 않고
     * 이력이 비어 있으면 NoSuchElementException 이 난다(리스너가 로그만 남긴다). 조건이 뒤집힌 것으로 보이지만
     * 동작을 바꾸면 회원 사진이 실제로 되돌아가기 시작하므로 이번 구조 변경에서는 건드리지 않았다.
     */
    fun rollbackMemberProfile(command: RollbackProfileCommand): MemberResult {
        val member = memberCommandService.getMemberById(command.memberId)

        val profiles = profilePort.getMemberProfiles(command.memberId)
        if (profiles != null && profiles.isEmpty()) {
            val profile = profiles.last()

            if (command.profileType == ProfileType.PROFILE_IMAGE || command.profileType == ProfileType.PROFILE_WALLPAPER) {
                val update = UpdateProfileCommand(
                    username = member.username,
                    statusMessage = member.statusMessage,
                    profileImage = if (profile.profileType == ProfileType.PROFILE_IMAGE) profile.value else member.profileImage,
                    wallpaperImage = if (profile.profileType == ProfileType.PROFILE_WALLPAPER) profile.value else member.wallpaperImage,
                )
                return memberCommandService.updateMemberProfile(member.userId!!, update)
            }
        }

        return member
    }

    private fun withProfiles(member: MemberResult): MemberProfileResult =
        MemberProfileResult(member, profilePort.getMemberProfiles(member.id!!))
}
