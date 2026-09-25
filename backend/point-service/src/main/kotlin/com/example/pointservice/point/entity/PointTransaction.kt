package com.example.pointservice.point.entity

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
import java.time.LocalDateTime

/**
 * 포인트 원장 한 줄. [amount] 는 부호가 있고(적립 +, 사용 −), [balanceAfter] 는 그 시점 잔액이다.
 * [refId] 는 호출 쪽이 주는 멱등 키(주문 번호, 초대 id 등) — 같은 사용자에 같은 refId 는 한 번만 적힌다.
 * 원장은 고치지 않으므로 updatedDate 가 없고, [createdDate] 는 서비스의 시계(한국 시간)로 적는다 —
 * 하루 상한 계산이 같은 시계를 쓰기 때문이다.
 */
@Entity
@Table(
    name = "point_transaction",
    indexes = [
        Index(name = "idx_point_tx_user_created", columnList = "user_id, created_date"),
        Index(name = "idx_point_tx_user_rule", columnList = "user_id, rule_code"),
    ],
    uniqueConstraints = [UniqueConstraint(name = "uk_point_tx_user_ref", columnNames = ["user_id", "ref_id"])],
)
class PointTransaction protected constructor() {

    @Id
    @Column(name = "point_transaction_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "user_id", nullable = false, length = 64)
    var userId: String = ""
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var type: PointTransactionType = PointTransactionType.EARN
        protected set

    @Column(nullable = false)
    var amount: Long = 0L
        protected set

    @Column(name = "balance_after", nullable = false)
    var balanceAfter: Long = 0L
        protected set

    @Column(name = "rule_code", length = 32)
    var ruleCode: String? = null
        protected set

    @Column(name = "ref_id", length = 128)
    var refId: String? = null
        protected set

    @Column(length = 200)
    var memo: String? = null
        protected set

    @Column(name = "created_date", nullable = false, updatable = false)
    var createdDate: LocalDateTime = LocalDateTime.MIN
        protected set

    constructor(
        userId: String,
        type: PointTransactionType,
        amount: Long,
        balanceAfter: Long,
        createdDate: LocalDateTime,
        ruleCode: String? = null,
        refId: String? = null,
        memo: String? = null,
    ) : this() {
        this.createdDate = createdDate
        this.userId = userId
        this.type = type
        this.amount = amount
        this.balanceAfter = balanceAfter
        this.ruleCode = ruleCode
        this.refId = refId
        this.memo = memo
    }
}
