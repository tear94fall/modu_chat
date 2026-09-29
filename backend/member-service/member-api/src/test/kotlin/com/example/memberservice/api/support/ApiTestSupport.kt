package com.example.memberservice.api.support

import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc

/**
 * DB 까지 가는 API 테스트의 바탕. 테스트는 @Transactional 을 쓰지 않는다 — 요청이 연 트랜잭션이 실제로 커밋돼야
 * ro 쪽(다른 커넥션)에서 보인다. 테스트가 끝나면 테이블을 비운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    @Autowired lateinit var mockMvc: MockMvc

    @Autowired @Qualifier("rwHikariDataSource") lateinit var rwHikariDataSource: HikariDataSource

    @AfterEach
    fun cleanDatabase() {
        DbCleaner.clean(rwHikariDataSource)
    }

    companion object {
        const val INTERNAL_TOKEN_HEADER = "X-Internal-Token"
        const val INTERNAL_TOKEN = "test-internal-token"
        const val AUTH_USER_ID_HEADER = "X-Auth-User-Id"
    }
}
