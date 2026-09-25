package com.example.memberservice.staff

import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.repository.MemberRepository
import com.example.memberservice.member.repository.MemberSort
import com.example.memberservice.member.service.MemberFriendService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 직원 지정·권한. 로그인(auth-service), 회원 조회(모두 인터널), 직원 관리(최상위 관리자)가 쓴다.
 *
 * 최상위 관리자는 자기 자신의 직원 정보를 바꾸거나 해제할 수 없다. 그러면 최상위 관리자가 모두 사라지는 일이 없다
 * (다른 최상위 관리자를 내리는 사람도 최상위 관리자이므로 적어도 한 명은 남는다).
 */
@Service
@Transactional(readOnly = true)
class StaffService(
    private val staffRepository: StaffRepository,
    private val memberRepository: MemberRepository,
    private val memberFriendService: MemberFriendService,
) {

    /** 로그인용. 탈퇴한 회원이거나 직원이 아니거나 권한이 없으면 null. */
    fun loginByEmail(email: String): StaffLoginDto? =
        memberRepository.findByEmail(email).orElse(null)?.let(::loginOf)

    /** 토큰 갱신용. 권한을 다시 읽어 바뀐 권한이 다음 토큰에 들어가게 한다. */
    fun loginByUserId(userId: String): StaffLoginDto? =
        memberRepository.findByUserId(userId).orElse(null)?.let(::loginOf)

    private fun loginOf(member: Member): StaffLoginDto? {
        if (member.isWithdrawn) return null
        val staff = staffRepository.findById(member.id!!).orElse(null) ?: return null
        if (staff.permissions.isEmpty()) return null
        return StaffLoginDto(member.userId, staff.sortedPermissions())
    }

    fun searchMembers(keyword: String?, sort: MemberSort, pageable: Pageable, staffOnly: Boolean): Page<StaffMemberSummaryDto> {
        val ids = if (staffOnly) staffRepository.findAll().map { it.memberId } else null
        val page = memberRepository.searchForAdmin(keyword, sort, pageable, ids)
        val staffById = staffRepository.findAllByMemberIdIn(page.content.mapNotNull { it.id }).associateBy { it.memberId }
        return page.map { StaffMemberSummaryDto.of(it, staffById[it.id]) }
    }

    fun memberDetail(id: Long): StaffMemberDetailDto {
        val member = memberRepository.findById(id).orElseThrow { StaffException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.") }
        val staff = staffRepository.findById(id).orElse(null)
        return StaffMemberDetailDto(
            member.id!!, member.userId, member.username, member.email, member.profileImage, member.statusMessage,
            member.createdDate, member.status, memberFriendService.listForAdmin(id).size, staff?.let(::infoOf),
        )
    }

    fun list(): List<StaffEntryDto> {
        val staff = staffRepository.findAll()
        val members = memberRepository.findAllById(staff.map { it.memberId }).associateBy { it.id }
        val names = modifierNames(staff)
        return staff.mapNotNull { s -> members[s.memberId]?.let { entryOf(it, s, names) } }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.username ?: it.email })
    }

    /** 직원으로 지정하거나 권한을 바꾼다. [actorUserId] 는 게이트웨이가 넣어 준 최상위 관리자 본인이다. */
    @Transactional
    fun setPermissions(actorUserId: String, memberId: Long, permissions: List<StaffPermission>?): StaffEntryDto {
        if (permissions.isNullOrEmpty()) {
            throw StaffException(HttpStatus.BAD_REQUEST, "권한을 하나 이상 고르세요. 직원에서 빼려면 직원 해제를 쓰세요.")
        }
        val member = memberRepository.findById(memberId).orElseThrow { StaffException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.") }
        rejectSelf(actorUserId, member)
        if (member.isWithdrawn) throw StaffException(HttpStatus.CONFLICT, "탈퇴한 회원은 직원으로 지정할 수 없습니다.")

        val staff = staffRepository.findById(memberId).orElse(null)
            ?.also { it.change(permissions.toSet(), actorUserId) }
            ?: staffRepository.save(Staff(memberId, permissions.toSet(), actorUserId))
        staffRepository.flush()
        return entryOf(member, staff, modifierNames(listOf(staff)))
    }

    @Transactional
    fun remove(actorUserId: String, memberId: Long) {
        val staff = staffRepository.findById(memberId).orElseThrow { StaffException(HttpStatus.NOT_FOUND, "직원이 아닙니다.") }
        memberRepository.findById(memberId).orElse(null)?.let { rejectSelf(actorUserId, it) }
        staffRepository.delete(staff)
    }

    private fun rejectSelf(actorUserId: String, target: Member) {
        if (target.userId == actorUserId) {
            throw StaffException(HttpStatus.CONFLICT, "자기 자신의 직원 권한은 바꿀 수 없습니다. 다른 최상위 관리자에게 요청하세요.")
        }
    }

    private fun modifierNames(staff: List<Staff>): Map<String, String?> {
        val ids = staff.mapNotNull { it.modifiedBy }.toSet()
        return ids.associateWith { id -> memberRepository.findByUserId(id).orElse(null)?.username }
    }

    private fun infoOf(staff: Staff) = StaffInfoDto(
        staff.sortedPermissions(), staff.createdDate, staff.modifiedDate, staff.modifiedBy,
        modifierNames(listOf(staff))[staff.modifiedBy],
    )

    private fun entryOf(member: Member, staff: Staff, names: Map<String, String?>) = StaffEntryDto(
        staff.memberId, member.userId, member.username, member.email, member.profileImage, staff.sortedPermissions(),
        staff.createdDate, staff.modifiedDate, staff.modifiedBy, names[staff.modifiedBy],
    )
}
