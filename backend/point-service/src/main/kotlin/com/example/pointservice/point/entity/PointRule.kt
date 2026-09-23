package com.example.pointservice.point.entity

import com.example.pointservice.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 적립 규칙. 다른 서비스는 규칙 코드만 보내고 점수·상한은 여기서 정한다 — 점수를 바꿀 때 호출 쪽을 고치지 않는다.
 * [dailyLimit]/[totalLimit] 은 사용자당 적립 횟수 상한(null 이면 무제한). 하루는 한국 시간 기준.
 */
@Entity
@Table(name = "point_rule")
class PointRule protected constructor() : BaseTimeEntity() {

    @Id
    @Column(name = "rule_code", length = 32)
    var code: String = ""
        protected set

    @Column(nullable = false, length = 100)
    var name: String = ""
        protected set

    @Column(nullable = false)
    var points: Long = 0L
        protected set

    @Column(name = "daily_limit")
    var dailyLimit: Int? = null
        protected set

    @Column(name = "total_limit")
    var totalLimit: Int? = null
        protected set

    @Column(nullable = false)
    var enabled: Boolean = true
        protected set

    constructor(code: String, name: String, points: Long, dailyLimit: Int? = null, totalLimit: Int? = null, enabled: Boolean = true) : this() {
        this.code = code
        this.name = name
        this.points = points
        this.dailyLimit = dailyLimit
        this.totalLimit = totalLimit
        this.enabled = enabled
    }

    fun update(name: String, points: Long, dailyLimit: Int?, totalLimit: Int?, enabled: Boolean) {
        this.name = name
        this.points = points
        this.dailyLimit = dailyLimit
        this.totalLimit = totalLimit
        this.enabled = enabled
    }
}
