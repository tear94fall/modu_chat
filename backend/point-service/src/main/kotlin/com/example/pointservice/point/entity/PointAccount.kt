package com.example.pointservice.point.entity

import com.example.pointservice.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * 사용자 한 명의 포인트 잔액. 사용자는 userId(구글 sub) 로만 식별한다 — 채팅 서비스의 member id 를 저장하지 않는다.
 * 잔액은 원장([PointTransaction]) 의 합과 같아야 하며, 변경은 행 잠금 아래에서만 한다.
 */
@Entity
@Table(
    name = "point_account",
    indexes = [Index(name = "uk_point_account_user", columnList = "user_id", unique = true)],
)
class PointAccount protected constructor() : BaseTimeEntity() {

    @Id
    @Column(name = "point_account_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "user_id", nullable = false, length = 64)
    var userId: String = ""
        protected set

    @Column(nullable = false)
    var balance: Long = 0L
        protected set

    constructor(userId: String) : this() {
        this.userId = userId
    }

    /** 잔액을 [delta] 만큼 바꾼다. 0 아래로는 내려가지 않는다(호출 쪽이 먼저 검사한다). */
    fun apply(delta: Long): Long {
        require(balance + delta >= 0L) { "잔액이 음수가 된다" }
        balance += delta
        return balance
    }
}
