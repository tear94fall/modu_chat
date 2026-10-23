package com.example.pointservice.application.domain.repository.ro

import com.example.pointservice.application.config.RoRepository
import com.example.pointservice.application.domain.entity.PointTransaction
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PointTransactionRoRepository : RoRepository<PointTransaction, Long> {

    /**
     * (user_id, ref_id) 유니크 인덱스를 타는 두 IN 조건. 사용자·refId 의 교차곱이 걸릴 수 있어 호출 쪽이
     * 요청한 (userId, refId) 쌍으로 다시 거른다.
     */
    fun findByUserIdInAndRefIdIn(userIds: Collection<String>, refIds: Collection<String>): List<PointTransaction>

    /** 종류·금액·시각만(백오피스 통계). [from, to) — 저장된 시각 기준(원장은 한국 시간, ClockConfig). 각 행은 [type, amount, createdDate]. */
    @Query("select t.type, t.amount, t.createdDate from PointTransaction t where t.createdDate >= :from and t.createdDate < :to")
    fun findTypeAmountCreatedBetween(@Param("from") from: LocalDateTime, @Param("to") to: LocalDateTime): List<Array<Any>>
}
