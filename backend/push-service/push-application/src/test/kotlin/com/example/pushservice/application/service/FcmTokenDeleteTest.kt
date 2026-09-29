package com.example.pushservice.application.service

import com.example.pushservice.application.domain.repository.rw.FcmTokenRwRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/** 회원 탈퇴 때 member-service 가 부른다. 그 userId 의 토큰 행을 전부 지운다(중복 행 포함). */
class FcmTokenDeleteTest {

    @Test
    fun deleteFcmToken_removesEveryRowOfTheUser() {
        val repository = mock<FcmTokenRwRepository>()
        val service = FcmTokenCommandService(repository)

        service.deleteFcmToken("withdrawn-user")

        verify(repository).deleteAllByUserId("withdrawn-user")
    }
}
