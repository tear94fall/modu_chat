package com.example.pushservice.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pushservice.fcm.service.FcmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PushAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FcmService fcmService;

    @Test
    void withoutToken_is403() throws Exception {
        mockMvc.perform(post("/api-admin/push/broadcast").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void broadcast_returnsGroupCount() throws Exception {
        when(fcmService.broadcast(any(), anyLong())).thenReturn(2);
        mockMvc.perform(post("/api-admin/push/broadcast").header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups").value(2));
    }

    @Test
    void unknownUser_is404() throws Exception {
        when(fcmService.searchFcmToken("nobody")).thenReturn(null);
        mockMvc.perform(post("/api-admin/push/users/nobody").header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isNotFound());
    }
}
