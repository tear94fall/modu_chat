package com.example.pushservice.application.usecase

import com.example.pushservice.application.service.FcmTokenCommandService
import com.example.pushservice.application.usecase.result.FcmTokenResult
import org.springframework.stereotype.Component

/** FCM 토큰 등록(앱)·삭제(회원 탈퇴). */
@Component
class PushTokenUseCase(private val fcmTokenCommandService: FcmTokenCommandService) {

    fun register(userId: String, fcmToken: String?): FcmTokenResult = fcmTokenCommandService.saveFcmToken(userId, fcmToken)

    fun delete(userId: String) = fcmTokenCommandService.deleteFcmToken(userId)
}
