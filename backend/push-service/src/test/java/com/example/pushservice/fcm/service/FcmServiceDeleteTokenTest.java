package com.example.pushservice.fcm.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.pushservice.fcm.repository.FcmRepository;
import org.junit.jupiter.api.Test;

/** 회원 탈퇴 때 member-service 가 부른다. 그 userId 의 토큰 행을 전부 지운다(중복 행 포함). */
class FcmServiceDeleteTokenTest {

    @Test
    void deleteFcmToken_removesEveryRowOfTheUser() {
        FcmRepository repository = mock(FcmRepository.class);
        FcmService service = new FcmService(repository);

        service.deleteFcmToken("withdrawn-user");

        verify(repository).deleteAllByUserId("withdrawn-user");
    }
}
