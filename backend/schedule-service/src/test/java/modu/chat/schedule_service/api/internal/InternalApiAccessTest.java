package modu.chat.schedule_service.api.internal;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 필터가 실제 컨텍스트에 등록돼 계층별로 적용되는지 확인한다.
 * 존재하지 않는 경로를 써서 서비스 레이어와 DB 를 건드리지 않는다:
 * 필터가 막으면 403, 통과하면 404 다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InternalApiAccessTest {

    @Autowired MockMvc mockMvc;

    @Test
    void internal_withoutToken_is403() throws Exception {
        mockMvc.perform(get("/api-internal/__probe__")).andExpect(status().isForbidden());
    }

    @Test
    void internal_withWrongToken_is403() throws Exception {
        mockMvc.perform(get("/api-internal/__probe__").header(InternalApiFilter.HEADER, "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internal_withToken_reachesDispatcher() throws Exception {
        mockMvc.perform(get("/api-internal/__probe__").header(InternalApiFilter.HEADER, "test-internal-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void debug_withoutToken_is403() throws Exception {
        mockMvc.perform(delete("/api-debug/__probe__")).andExpect(status().isForbidden());
    }

    @Test
    void public_withoutToken_isNotBlockedByFilter() throws Exception {
        int status = mockMvc.perform(get("/api-public/__probe__")).andReturn().getResponse().getStatus();
        assertNotEquals(403, status);
    }

    @Test
    void doubleSlash_internal_withoutToken_is403() throws Exception {
        // MockMvcRequestBuilders.get(String) 은 URI 파서를 거치는데, 맨 앞 "//" 를
        // network-path reference(권한부 시작)로 해석해 "api-internal" 을 host 로 삼키고
        // 경로를 "/__probe__" 로 잘라버린다 (실제 서버는 요청줄의 경로를 그대로 둔다 — 이게
        // 애초에 B 가 고치는 우회다). RequestPostProcessor 로 파싱 뒤 raw requestURI 를
        // 다시 덮어써 실제 우회 시나리오("//api-internal/...")를 재현한다.
        mockMvc.perform(get("/api-internal/__probe__").with(req -> {
                    req.setRequestURI("//api-internal/__probe__");
                    return req;
                }))
                .andExpect(status().isForbidden());
    }
}
