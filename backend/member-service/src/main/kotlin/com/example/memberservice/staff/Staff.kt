package com.example.memberservice.staff

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 직원. 행이 있으면 직원이다. 회원(member) 한 명과 1:1 이고 키도 member_id 를 그대로 쓴다.
 *
 * 로그인은 회원의 구글 계정으로 한다(비밀번호를 두지 않는다). 권한은 staff_permission 에 한 줄씩 있다.
 * 시각은 서버 시계(UTC) 기준이다.
 */
@Entity
@Table(name = "staff")
class Staff(
    @Id
    @Column(name = "member_id")
    val memberId: Long,

    permissions: Set<StaffPermission>,

    /** 마지막으로 바꾼 최상위 관리자의 userId. DB 에서 직접 넣은 첫 최상위 관리자는 null 이다. */
    modifiedBy: String?,
) {
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "staff_permission", joinColumns = [JoinColumn(name = "member_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 16, nullable = false)
    var permissions: MutableSet<StaffPermission> = permissions.toMutableSet()
        protected set

    @Column(name = "created_date", nullable = false, updatable = false)
    var createdDate: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "modified_date", nullable = false)
    var modifiedDate: LocalDateTime = createdDate
        protected set

    @Column(name = "modified_by", length = 64)
    var modifiedBy: String? = modifiedBy
        protected set

    fun change(permissions: Set<StaffPermission>, by: String) {
        this.permissions.clear()
        this.permissions.addAll(permissions)
        this.modifiedDate = LocalDateTime.now()
        this.modifiedBy = by
    }
}
