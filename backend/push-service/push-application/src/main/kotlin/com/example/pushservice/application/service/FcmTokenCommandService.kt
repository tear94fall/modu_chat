package com.example.pushservice.application.service

import com.example.pushservice.application.config.RwJpaConfig
import com.example.pushservice.application.domain.entity.FcmToken
import com.example.pushservice.application.domain.repository.rw.FcmTokenRwRepository
import com.example.pushservice.application.usecase.result.FcmTokenResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** FCM 토큰 쓰기(master). 있는지 보는 검사도 master 에서 한다 — 레플리카가 늦으면 같은 회원의 행이 또 생긴다. */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class FcmTokenCommandService(private val fcmTokenRwRepository: FcmTokenRwRepository) {

    /**
     * userId 당 한 행만 유지되도록 upsert 한다(과거에는 로그인마다 새 행을 insert 해서 중복이 쌓였다).
     * 기존 행이 있으면 토큰만 갱신하고, 혹시 남아있는 다른 중복 행은 이번에 정리한다.
     */
    fun saveFcmToken(userId: String?, fcmToken: String?): FcmTokenResult {
        val saved = fcmTokenRwRepository.findFirstByUserIdOrderByIdDesc(userId)
            .map { existing ->
                existing.fcmToken = fcmToken
                fcmTokenRwRepository.save(existing)
            }
            .orElseGet { fcmTokenRwRepository.save(FcmToken(userId, fcmToken)) }
        fcmTokenRwRepository.deleteByUserIdAndIdNot(userId, saved.id)
        return FcmTokenResult.from(saved)
    }

    /** 회원 탈퇴 때 member-service 가 부른다. 토큰이 없어도 조용히 지나간다. */
    fun deleteFcmToken(userId: String) {
        fcmTokenRwRepository.deleteAllByUserId(userId)
    }
}
