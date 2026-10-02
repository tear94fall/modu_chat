package modu.chat.schedule_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 쿠버네티스 probe·compose healthcheck 가 보는 경로. 토큰·인증 없이 200 + "UP" 이어야 한다.
 * readiness 는 master(rw) DB 만 본다 — 자동 db indicator(ro 포함)는 readiness 에 없다.
 * 그룹 구성은 웹 응답(show-details: never)에 안 보이므로 HealthEndpoint 로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthProbesTest {

    @Autowired MockMvc mockMvc;
    @Autowired HealthEndpoint healthEndpoint;

    @Test
    void liveness_isUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readiness_isUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readiness_group_hasExpectedIndicators() {
        CompositeHealth readiness = (CompositeHealth) healthEndpoint.healthForPath("readiness");
        assertThat(readiness.getComponents().keySet()).containsExactlyInAnyOrder("readinessState", "masterDb");
        assertThat(readiness.getComponents().get("readinessState").getStatus()).isEqualTo(Status.UP);
        assertThat(readiness.getComponents().get("masterDb").getStatus()).isEqualTo(Status.UP);
    }
}
