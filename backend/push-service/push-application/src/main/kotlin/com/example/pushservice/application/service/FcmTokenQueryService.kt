package com.example.pushservice.application.service

import com.example.pushservice.application.config.RoJpaConfig
import com.example.pushservice.application.domain.repository.ro.FcmTokenRoRepository
import com.example.pushservice.application.usecase.result.FcmTokenResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 발송 대상 토큰 조회(replica). 토큰을 쓰는 쪽(앱 로그인·탈퇴)은 쓴 뒤에 다시 읽지 않고, 발송은 그와 무관한 때에 일어난다.
 * 복제가 따라오기 전의 짧은 순간에는 방금 바뀐 토큰 대신 옛 토큰이 보일 수 있다(그 푸시 한 건이 빠진다).
 */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class FcmTokenQueryService(private val fcmTokenRoRepository: FcmTokenRoRepository) {

    fun searchFcmToken(userId: String): FcmTokenResult? =
        fcmTokenRoRepository.findFirstByUserIdOrderByIdDesc(userId).map { FcmTokenResult.from(it) }.orElse(null)

    fun searchAllFcmToken(): List<FcmTokenResult> = fcmTokenRoRepository.findAll().map { FcmTokenResult.from(it) }
}
