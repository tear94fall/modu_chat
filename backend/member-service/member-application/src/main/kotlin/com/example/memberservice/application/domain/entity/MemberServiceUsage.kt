package com.example.memberservice.application.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Duration
import java.time.LocalDateTime

/**
 * 회원 한 명이 서비스 하나를 처음·마지막으로 쓴 시각(UTC). (user_id, service) 당 한 줄이다.
 * 회원이 탈퇴해도 지우지 않는다(이력).
 */
@Entity
@Table(
    name = "member_service_usage",
    uniqueConstraints = [UniqueConstraint(name = "uk_member_service_usage", columnNames = ["user_id", "service"])],
    indexes = [Index(name = "idx_member_service_usage_service_user", columnList = "service, user_id")],
)
class MemberServiceUsage protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    /** member.user_id 와 같은 값. */
    @Column(name = "user_id", nullable = false, length = 255)
    lateinit var userId: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "service", nullable = false, length = 16)
    lateinit var service: ModuService
        protected set

    @Column(name = "first_used_at", nullable = false)
    lateinit var firstUsedAt: LocalDateTime
        protected set

    @Column(name = "last_used_at", nullable = false)
    lateinit var lastUsedAt: LocalDateTime
        protected set

    constructor(userId: String, service: ModuService, firstUsedAt: LocalDateTime, lastUsedAt: LocalDateTime = firstUsedAt) : this() {
        this.userId = userId
        this.service = service
        this.firstUsedAt = firstUsedAt
        this.lastUsedAt = lastUsedAt
    }

    /**
     * 마지막 이용 시각을 now 로 올린다. 토큰을 갱신할 때마다 쓰지 않도록 [THROTTLE] 보다 오래됐을 때만 바꾼다.
     * 바꿨으면 true.
     */
    fun touch(now: LocalDateTime): Boolean {
        if (lastUsedAt.isAfter(now.minus(THROTTLE))) {
            return false
        }
        lastUsedAt = now
        return true
    }

    companion object {
        @JvmField
        val THROTTLE: Duration = Duration.ofHours(1)
    }
}
