package com.example.pushservice.application.usecase.result

import com.example.pushservice.application.domain.entity.FcmToken

data class FcmTokenResult(
    val id: Long?,
    val userId: String?,
    val fcmToken: String?,
) {
    companion object {
        fun from(token: FcmToken): FcmTokenResult = FcmTokenResult(token.id, token.userId, token.fcmToken)
    }
}
