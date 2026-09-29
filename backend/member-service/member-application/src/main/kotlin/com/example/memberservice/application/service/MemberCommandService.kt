package com.example.memberservice.application.service

import com.example.memberservice.application.common.exception.CustomException
import com.example.memberservice.application.common.exception.ErrorCode
import com.example.memberservice.application.common.lock.ApiLock
import com.example.memberservice.application.common.lock.LockParam
import com.example.memberservice.application.config.RwJpaConfig
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.ProfileType
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.domain.repository.query.FriendSort
import com.example.memberservice.application.domain.repository.rw.MemberFriendRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberServiceUsageRwRepository
import com.example.memberservice.application.domain.repository.rw.StaffRwRepository
import com.example.memberservice.application.usecase.command.AddMemberProfileCommand
import com.example.memberservice.application.usecase.command.ChatRoomMembersCommand
import com.example.memberservice.application.usecase.command.GoogleAccountCommand
import com.example.memberservice.application.usecase.command.UpdateProfileCommand
import com.example.memberservice.application.usecase.result.AdminMemberDetailResult
import com.example.memberservice.application.usecase.result.GoogleMemberResult
import com.example.memberservice.application.usecase.result.MemberResult
import com.example.memberservice.application.usecase.result.WithdrawalTarget
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회원 쓰기(master)와, 쓰기 직후에 읽히는 조회.
 *
 * 다른 서비스 호출은 여기서 하지 않는다 — 유스케이스가 트랜잭션 밖에서 하고, 그 앞뒤의 DB 작업만 여기로 온다.
 *
 * "쓰기 직후 읽기" ([getUserById], [getMemberByEmail], [getMemberDetailByUserId]): 읽기 전용이지만 master 로 읽는다.
 * auth-service 가 방금 만든 회원을 바로 쓰고, 앱이 첫 로그인 직후 이메일로 자기 정보를 읽기 때문이다.
 */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class MemberCommandService(
    private val memberRwRepository: MemberRwRepository,
    private val memberFriendRwRepository: MemberFriendRwRepository,
    private val staffRwRepository: StaffRwRepository,
    private val usageRwRepository: MemberServiceUsageRwRepository,
) {

    /** auth-service·chat-service 가 가입·로그인 직후에 읽는다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getUserById(userId: String): MemberResult = MemberResult.from(findByUserId(userId))

    /** 앱이 로그인 직후(첫 로그인이면 가입 직후) 읽는다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getMemberByEmail(email: String): MemberResult =
        memberRwRepository.findByEmail(email).map(MemberResult::from)
            .orElseThrow { CustomException(ErrorCode.EMAIL_NOT_FOUND, email) }

    /** 프로필 되돌리기(Kafka) 전에 지금 상태를 읽는다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getMemberById(id: Long): MemberResult = MemberResult.from(findById(id))

    /** 백오피스에서 로그인한 본인 정보. 내 정보 수정 직후에도 다시 읽는다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun getMemberDetailByUserId(userId: String): AdminMemberDetailResult = detailOf(findByUserId(userId))

    /**
     * auth-service 가 구글 ID 토큰을 검증한 뒤 부른다. 이메일로 찾고 없으면 만든다(가입 = 첫 로그인).
     * 탈퇴했던 계정(같은 구글 sub)이면 새 행을 만들지 않고 그 행을 되살린다.
     * 같은 계정의 동시 요청은 락으로 줄 세운다. 구글 사진 올리기는 락·트랜잭션 밖에서 유스케이스가 한다.
     */
    @ApiLock
    fun findOrCreateGoogleMember(@LockParam account: GoogleAccountCommand): GoogleMemberResult {
        val registered = memberRwRepository.findByEmail(account.email!!)
        if (registered.isPresent) {
            return GoogleMemberResult(MemberResult.from(registered.get()), fresh = false)
        }

        // 탈퇴했던 계정(같은 구글 sub)이면 새 행을 만들지 않고 그 행을 되살린다. uk_member_user_id 때문에도 그래야 한다.
        val withdrawn = memberRwRepository.findByUserId(account.sub!!).filter { it.isWithdrawn }
        if (withdrawn.isPresent) {
            val member = withdrawn.get()
            member.reactivate(account.email, account.name)
            return GoogleMemberResult(MemberResult.from(member), fresh = true, revived = true)
        }

        return GoogleMemberResult(registerMember(account), fresh = true)
    }

    /** 검증된 계정으로 신규 회원만 만든다. 락과 멱등 처리는 findOrCreateGoogleMember 가 맡는다. */
    fun registerMember(account: GoogleAccountCommand): MemberResult {
        val member = Member(
            userId = account.sub,
            email = account.email,
            auth = "google",
            role = Role.ROLE_MEMBER,
            username = account.name,
            statusMessage = "",
            profileImage = account.picture ?: "",
            wallpaperImage = "",
            profiles = mutableListOf(),
            chatRoomMembers = mutableListOf(),
        )

        // 동시에 들어온 두 요청이 모두 findByEmail 을 통과한 뒤 저장을 시도하면 DB 의 유일 제약
        // (uk_member_email/uk_member_user_id) 이 하나만 통과시키고 나머지는 DataIntegrityViolationException
        // 을 던진다. 같은 트랜잭션 안에서는 REPEATABLE READ 스냅샷 때문에 상대가 커밋한 행이 보이지
        // 않고 트랜잭션도 이미 롤백 표시가 되어 여기서 복구할 수 없다 — 그대로 흘려보내고
        // MemberSignupUseCase 가 트랜잭션 밖에서 재시도한다.
        return MemberResult.from(memberRwRepository.save(member))
    }

    /** storage·profile 에 올린 구글 사진을 회원에 반영한다. */
    fun applyProfileImage(memberId: Long, profileType: ProfileType?, value: String?, profileId: Long): MemberResult {
        val member = findById(memberId)
        member.updateMemberInfo(profileType, value)
        member.addProfile(profileId)
        return MemberResult.from(member)
    }

    /**
     * 가입(또는 되살리기) 뒤 구글 사진 단계가 실패했을 때 되돌린다 — 예전에는 한 트랜잭션이라 함께 롤백됐다.
     * 새 회원은 지우고, 되살린 회원은 다시 탈퇴 상태로 돌린다. 다음 로그인 때 처음부터 다시 한다.
     */
    fun cancelSignup(memberId: Long, revived: Boolean) {
        val member = memberRwRepository.findById(memberId).orElse(null) ?: return
        if (revived) {
            member.withdraw()
        } else {
            memberRwRepository.delete(member)
        }
    }

    /** 앱의 프로필 수정. 네 필드를 통째로 덮어쓴다. */
    fun updateMemberProfile(userId: String, command: UpdateProfileCommand): MemberResult {
        val member = findByUserId(userId)
        member.updateProfile(command.username, command.statusMessage, command.profileImage, command.wallpaperImage)
        return MemberResult.from(member)
    }

    /**
     * 백오피스에서 본인 정보를 고친다. null 인 필드는 기존 값을 유지한다 —
     * 엔티티의 updateProfile 은 네 필드를 통째로 덮어쓰기 때문이다. 고친 뒤의 상세를 같은 트랜잭션에서 읽어 돌려준다.
     */
    fun updateMyProfile(userId: String, command: UpdateProfileCommand): AdminMemberDetailResult {
        val member = findByUserId(userId)
        member.updateProfile(
            command.username ?: member.username,
            command.statusMessage ?: member.statusMessage,
            command.profileImage ?: member.profileImage,
            command.wallpaperImage ?: member.wallpaperImage,
        )
        return detailOf(member)
    }

    fun inviteMembers(command: ChatRoomMembersCommand): List<MemberResult> =
        memberRwRepository.findAllById(command.memberIds)
            .onEach { it.addChatRoom(command.chatRoomId) }
            .map(MemberResult::from)

    fun exitMembers(command: ChatRoomMembersCommand): List<MemberResult> =
        memberRwRepository.findAllById(command.memberIds)
            .onEach { it.delChatRoom(command.chatRoomId) }
            .map(MemberResult::from)

    fun addMemberProfile(command: AddMemberProfileCommand): Long {
        val member = findById(command.memberId)
        member.addProfile(command.profileId)
        return member.profiles!!.last()
    }

    /**
     * 탈퇴할 회원을 읽는다. 없는 회원이면 MEMBER_ID_NOT_FOUND_ERROR, 이미 탈퇴했으면 null(할 일이 없다).
     * 탈퇴 여부는 master 로 본다 — 방금 탈퇴한 회원을 다시 정리하지 않도록.
     */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun findWithdrawalTarget(userId: String): WithdrawalTarget? {
        val member = memberRwRepository.findByUserId(userId)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, userId) }
        if (member.isWithdrawn) {
            return null
        }
        return WithdrawalTarget(member.id!!, member.userId, member.profileImage, member.wallpaperImage)
    }

    /** 탈퇴의 DB 작업: 친구 관계(양쪽)를 지우고 회원을 탈퇴 상태로 바꾼다(개인정보는 비운다). 한 트랜잭션이다. */
    fun completeWithdrawal(memberId: Long) {
        val member = findById(memberId)
        if (member.isWithdrawn) {
            return
        }
        memberFriendRwRepository.deleteAllByMember_IdOrFriend_Id(memberId, memberId)
        member.withdraw()
    }

    private fun detailOf(member: Member): AdminMemberDetailResult = AdminMemberDetails.of(
        member,
        memberFriendRwRepository.findPage(member.id!!, FriendSort.NAME_ASC, Pageable.unpaged()).content,
        staffRwRepository::findAllByMemberIdIn,
        usageRwRepository.findAllByUserId(member.userId),
    )

    private fun findByUserId(userId: String): Member =
        memberRwRepository.findByUserId(userId)
            .orElseThrow { CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId) }

    private fun findById(id: Long): Member =
        memberRwRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id) }
}
