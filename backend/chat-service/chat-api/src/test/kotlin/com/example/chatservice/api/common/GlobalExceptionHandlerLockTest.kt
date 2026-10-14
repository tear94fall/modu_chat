package com.example.chatservice.api.common

import com.example.chatservice.application.common.lock.ApiLockAcquisitionException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler

/** Redis 락(@ApiLock)을 대기 시간 안에 못 잡으면 503 "요청이 몰려…" 다. */
class GlobalExceptionHandlerLockTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun apiLockAcquisitionFailure_is503Busy() {
        val response = handler.handleLockFailure(ApiLockAcquisitionException("LOCK:chat:room-create:1,2"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(response.body).isEqualTo(ErrorResponse("요청이 몰려 처리하지 못했어요. 잠시 후 다시 시도해 주세요.", "BUSY"))
    }

    /** 503 은 Redis 락 전용이다. DB 행 잠금(비관적 잠금)은 더 이상 쓰지 않으므로 그 예외들은 매핑하지 않는다. */
    @Test
    fun only_theRedisLockException_isMappedToBusy() {
        val handled = GlobalExceptionHandler::class.java
            .getMethod("handleLockFailure", Exception::class.java)
            .getAnnotation(ExceptionHandler::class.java)
            .value

        assertThat(handled).containsExactly(ApiLockAcquisitionException::class)
    }
}
