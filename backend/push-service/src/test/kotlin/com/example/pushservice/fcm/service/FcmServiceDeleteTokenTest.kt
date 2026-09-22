package com.example.pushservice.fcm.service

import com.example.pushservice.fcm.repository.FcmRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/** 회원 탈퇴 때 member-service 가 부른다. 그 userId 의 토큰 행을 전부 지운다(중복 행 포함). */
class FcmServiceDeleteTokenTest {

    @Test
    fun deleteFcmToken_removesEveryRowOfTheUser() {
        val repository = mock<FcmRepository>()
        val service = FcmService(repository)

        service.deleteFcmToken("withdrawn-user")

        verify(repository).deleteAllByUserId("withdrawn-user")
    }
}
