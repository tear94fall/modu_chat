package com.example.pushservice.fcm.service;

import com.example.pushservice.fcm.dto.FcmUserMessageDto;
import com.example.pushservice.fcm.entity.FcmToken;
import com.example.pushservice.fcm.repository.FcmRepository;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 반응 알림처럼 한 사람에게만 보내는 푸시. */
class FcmServiceUserMessageTest {

    @Test
    void sendsToTheUsersTokenWithData() throws Exception {
        FcmRepository repository = mock(FcmRepository.class);
        when(repository.findFirstByUserIdOrderByIdDesc("author")).thenReturn(Optional.of(new FcmToken("author", "tok")));
        FcmService service = spy(new FcmService(repository));
        doNothing().when(service).sendMessage(any(Message.class));

        boolean sent = service.sendUserMessageWithData(new FcmUserMessageDto("author", "방", "준섭님이 👍 반응을 남겼습니다", Map.of("roomId", "r1")));

        assertTrue(sent);
        verify(service).sendMessage(any(Message.class));
    }

    @Test
    void withoutTokenSendsNothing() throws Exception {
        FcmRepository repository = mock(FcmRepository.class);
        when(repository.findFirstByUserIdOrderByIdDesc("nobody")).thenReturn(Optional.empty());
        FcmService service = spy(new FcmService(repository));

        assertFalse(service.sendUserMessageWithData(new FcmUserMessageDto("nobody", "방", "b", Map.of())));
        verify(service, never()).sendMessage(any(Message.class));
    }
}
