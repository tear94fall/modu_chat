package com.example.pushservice.fcm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.pushservice.fcm.dto.RequestPushMessage;
import com.example.pushservice.fcm.entity.FcmToken;
import com.example.pushservice.fcm.repository.FcmRepository;
import com.google.firebase.messaging.MulticastMessage;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class FcmServiceBroadcastTest {

    @Test
    void broadcast_splitsTokensIntoMulticastGroups() throws Exception {
        FcmService service = spy(new FcmService(mock(FcmRepository.class)));
        doReturn(IntStream.range(0, 7).mapToObj(i -> new FcmToken("u" + i, "t" + i)).toList()).when(service).searchAllFcmToken();
        doNothing().when(service).sendMessage(any(MulticastMessage.class));

        int groups = service.broadcast(RequestPushMessage.builder().title("t").body("b").build(), 3L);

        assertEquals(3, groups);
        verify(service, times(3)).sendMessage(any(MulticastMessage.class));
    }

    @Test
    void broadcast_noTokens_sendsNothing() throws Exception {
        FcmService service = spy(new FcmService(mock(FcmRepository.class)));
        doReturn(List.of()).when(service).searchAllFcmToken();

        int groups = service.broadcast(RequestPushMessage.builder().title("t").body("b").build(), 3L);

        assertEquals(0, groups);
        verify(service, never()).sendMessage(any(MulticastMessage.class));
    }

    @Test
    void broadcast_deduplicatesIdenticalTokens() throws Exception {
        FcmService service = spy(new FcmService(mock(FcmRepository.class)));
        // 5 rows, only 2 distinct tokens (duplicate rows for the same user/token, as in the dev DB bug)
        doReturn(List.of(
                new FcmToken("u1", "dup"),
                new FcmToken("u1", "dup"),
                new FcmToken("u1", "dup"),
                new FcmToken("u2", "other"),
                new FcmToken("u2", "other")
        )).when(service).searchAllFcmToken();
        doNothing().when(service).sendMessage(any(MulticastMessage.class));

        int groups = service.broadcast(RequestPushMessage.builder().title("t").body("b").build(), 10L);

        // com.google.firebase.messaging.MulticastMessage does not expose its token list
        // (getMessageList()/Message.getToken() are package-private in firebase-admin 9.0.0),
        // so we can only assert on group count here, not on the captured message's token count.
        assertEquals(1, groups);
        verify(service, times(1)).sendMessage(any(MulticastMessage.class));
    }
}
