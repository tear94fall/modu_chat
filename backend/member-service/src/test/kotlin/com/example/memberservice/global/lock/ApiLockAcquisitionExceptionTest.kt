package com.example.memberservice.global.lock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 락을 얻지 못한 것은 서버 오류가 아니라 경합이다. 500 이 아니라 409 로 나가는지 고정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiLockAcquisitionExceptionTest.LockConflictProbeController::class)
class ApiLockAcquisitionExceptionTest {

    /** 락 실패를 실제 요청으로 재현하기 위한 테스트 전용 엔드포인트. */
    @RestController
    class LockConflictProbeController {

        @GetMapping("/__lock_conflict_probe__")
        fun throwLockConflict(): String {
            throw ApiLockAcquisitionException("LOCK:probe")
        }
    }

    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun notAcquired_isConflictNotServerError() {
        mockMvc.perform(get("/__lock_conflict_probe__"))
            .andExpect(status().isConflict)
    }

    @Test
    fun exceptionIsAnnotatedWithConflict() {
        val responseStatus = ApiLockAcquisitionException::class.java.getAnnotation(ResponseStatus::class.java)

        assertNotNull(responseStatus, "@ResponseStatus 가 없으면 500 으로 나간다")
        assertEquals(HttpStatus.CONFLICT, responseStatus.value)
        assertTrue(responseStatus.reason.contains("처리 중"))
    }

    @Test
    fun messageCarriesTheKey() {
        val exception = ApiLockAcquisitionException("LOCK:member:abc")

        assertTrue(exception.message!!.contains("LOCK:member:abc"))
    }
}
