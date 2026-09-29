package com.example.memberservice.application.service

import com.example.memberservice.application.common.exception.StaffException
import com.example.memberservice.application.config.RwJpaConfig
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.Staff
import com.example.memberservice.application.domain.repository.rw.MemberFriendRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.domain.repository.rw.StaffRwRepository
import com.example.memberservice.application.usecase.command.SetStaffPermissionsCommand
import com.example.memberservice.application.usecase.result.StaffEntryResult
import com.example.memberservice.application.usecase.result.StaffInfoResult
import com.example.memberservice.application.usecase.result.StaffLoginResult
import com.example.memberservice.application.usecase.result.StaffMemberDetailResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 직원 지정·권한(master). 로그인(auth-service), 회원 상세(모두 인터널), 직원 관리(최상위 관리자)가 쓴다.
 *
 * 읽기도 master 다: 로그인·토큰 갱신은 방금 바뀐 권한을 봐야 하고, 콘솔은 권한을 저장한 직후에 직원 목록과
 * 회원 상세를 다시 읽는다.
 *
 * 최상위 관리자는 자기 자신의 직원 정보를 바꾸거나 해제할 수 없다. 그러면 최상위 관리자가 모두 사라지는 일이 없다
 * (다른 최상위 관리자를 내리는 사람도 최상위 관리자이므로 적어도 한 명은 남는다).
 */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class StaffCommandService(
    private val staffRwRepository: StaffRwRepository,
    private val memberRwRepository: MemberRwRepository,
    private val memberFriendRwRepository: MemberFriendRwRepository,
) {

    /** 로그인용. 탈퇴한 회원이거나 직원이 아니거나 권한이 없으면 null. */
    fun loginByEmail(email: String): StaffLoginResult? =
        memberRwRepository.findByEmail(email).orElse(null)?.let(::loginOf)

    /** 토큰 갱신용. 권한을 다시 읽어 바뀐 권한이 다음 토큰에 들어가게 한다. */
    fun loginByUserId(userId: String): StaffLoginResult? =
        memberRwRepository.findByUserId(userId).orElse(null)?.let(::loginOf)

    private fun loginOf(member: Member): StaffLoginResult? {
        if (member.isWithdrawn) return null
        val staff = staffRwRepository.findById(member.id!!).orElse(null) ?: return null
        if (staff.permissions.isEmpty()) return null
        return StaffLoginResult(member.userId, staff.sortedPermissions())
    }

    fun memberDetail(id: Long): StaffMemberDetailResult {
        val member = memberRwRepository.findById(id).orElseThrow { StaffException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.") }
        val staff = staffRwRepository.findById(id).orElse(null)
        return StaffMemberDetailResult(
            member.id!!, member.userId, member.username, member.email, member.profileImage, member.statusMessage,
            member.createdDate, member.status, memberFriendRwRepository.countByMemberId(id).toInt(), staff?.let(::infoOf),
        )
    }

    fun list(): List<StaffEntryResult> {
        val staff = staffRwRepository.findAll()
        val members = memberRwRepository.findAllById(staff.map { it.memberId }).associateBy { it.id }
        val names = modifierNames(staff)
        return staff.mapNotNull { s -> members[s.memberId]?.let { entryOf(it, s, names) } }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.username ?: it.email })
    }

    /** 직원으로 지정하거나 권한을 바꾼다. actorUserId 는 게이트웨이가 넣어 준 최상위 관리자 본인이다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
    fun setPermissions(command: SetStaffPermissionsCommand): StaffEntryResult {
        val (actorUserId, memberId, permissions) = command
        if (permissions.isNullOrEmpty()) {
            throw StaffException(HttpStatus.BAD_REQUEST, "권한을 하나 이상 고르세요. 직원에서 빼려면 직원 해제를 쓰세요.")
        }
        val member = memberRwRepository.findById(memberId).orElseThrow { StaffException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.") }
        rejectSelf(actorUserId, member)
        if (member.isWithdrawn) throw StaffException(HttpStatus.CONFLICT, "탈퇴한 회원은 직원으로 지정할 수 없습니다.")

        val staff = staffRwRepository.findById(memberId).orElse(null)
            ?.also { it.change(permissions.toSet(), actorUserId) }
            ?: staffRwRepository.save(Staff(memberId, permissions.toSet(), actorUserId))
        staffRwRepository.flush()
        return entryOf(member, staff, modifierNames(listOf(staff)))
    }

    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
    fun remove(actorUserId: String, memberId: Long) {
        val staff = staffRwRepository.findById(memberId).orElseThrow { StaffException(HttpStatus.NOT_FOUND, "직원이 아닙니다.") }
        memberRwRepository.findById(memberId).orElse(null)?.let { rejectSelf(actorUserId, it) }
        staffRwRepository.delete(staff)
    }

    private fun rejectSelf(actorUserId: String, target: Member) {
        if (target.userId == actorUserId) {
            throw StaffException(HttpStatus.CONFLICT, "자기 자신의 직원 권한은 바꿀 수 없습니다. 다른 최상위 관리자에게 요청하세요.")
        }
    }

    private fun modifierNames(staff: List<Staff>): Map<String, String?> {
        val ids = staff.mapNotNull { it.modifiedBy }.toSet()
        return ids.associateWith { id -> memberRwRepository.findByUserId(id).orElse(null)?.username }
    }

    private fun infoOf(staff: Staff) = StaffInfoResult(
        staff.sortedPermissions(), staff.createdDate, staff.modifiedDate, staff.modifiedBy,
        modifierNames(listOf(staff))[staff.modifiedBy],
    )

    private fun entryOf(member: Member, staff: Staff, names: Map<String, String?>) = StaffEntryResult(
        staff.memberId, member.userId, member.username, member.email, member.profileImage, staff.sortedPermissions(),
        staff.createdDate, staff.modifiedDate, staff.modifiedBy, names[staff.modifiedBy],
    )
}
