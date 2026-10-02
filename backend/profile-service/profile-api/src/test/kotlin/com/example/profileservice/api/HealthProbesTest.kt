package com.example.profileservice.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.CompositeHealth
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 쿠버네티스 probe·compose healthcheck 가 보는 경로. 토큰·인증 없이 200 + "UP" 이어야 한다.
 * readiness 는 master(rw) DB 만 본다 — 자동 db indicator(ro 포함)는 readiness 에 없다. 그룹 구성은 웹 응답(show-details: never)에 안 보이므로 HealthEndpoint 로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthProbesTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var healthEndpoint: HealthEndpoint

    @Test
    fun liveness_isUp() {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    @Test
    fun readiness_isUp() {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    @Test
    fun readiness_group_hasExpectedIndicators() {
        val readiness = healthEndpoint.healthForPath("readiness") as CompositeHealth
        assertThat(readiness.components.keys).containsExactlyInAnyOrder("readinessState", "masterDb")
        assertThat(readiness.components["readinessState"]!!.status).isEqualTo(Status.UP)
        assertThat(readiness.components["masterDb"]!!.status).isEqualTo(Status.UP)
    }
}
