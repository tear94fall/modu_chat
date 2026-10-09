package com.example.pointservice.application.domain.repository.ro

import com.example.pointservice.application.config.RoRepository
import com.example.pointservice.application.domain.entity.PointTransaction

interface PointTransactionRoRepository : RoRepository<PointTransaction, Long> {

    /**
     * (user_id, ref_id) 유니크 인덱스를 타는 두 IN 조건. 사용자·refId 의 교차곱이 걸릴 수 있어 호출 쪽이
     * 요청한 (userId, refId) 쌍으로 다시 거른다.
     */
    fun findByUserIdInAndRefIdIn(userIds: Collection<String>, refIds: Collection<String>): List<PointTransaction>
}
