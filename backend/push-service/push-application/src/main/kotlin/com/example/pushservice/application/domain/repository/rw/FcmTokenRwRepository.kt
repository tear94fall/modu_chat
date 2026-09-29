package com.example.pushservice.application.domain.repository.rw

import com.example.pushservice.application.config.RwRepository
import com.example.pushservice.application.domain.entity.FcmToken
import java.util.Optional

interface FcmTokenRwRepository : RwRepository<FcmToken, Long> {

    /** userId 당 여러 행이 남아있을 수 있어(과거 버그) 가장 최근 것 하나만 가져온다. */
    fun findFirstByUserIdOrderByIdDesc(userId: String?): Optional<FcmToken>

    /** upsert 후 같은 userId 의 나머지(오래된) 중복 행을 정리한다. */
    fun deleteByUserIdAndIdNot(userId: String?, id: Long?)

    /** 회원 탈퇴: 그 userId 의 행을 전부 지운다(과거 중복 행 포함). */
    fun deleteAllByUserId(userId: String)
}
