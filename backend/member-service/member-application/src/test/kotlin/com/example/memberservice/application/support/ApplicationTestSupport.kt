package com.example.memberservice.application.support

import com.example.memberservice.application.port.ChatRoomPort
import com.example.memberservice.application.port.CommercePort
import com.example.memberservice.application.port.ProfilePort
import com.example.memberservice.application.port.PushPort
import com.example.memberservice.application.port.StoragePort
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * member-application 모듈 통합 테스트의 바탕. 바깥 호출 포트는 모두 목이다(구현은 member-api 에 있다).
 * replica(ro) 로 읽는 흐름을 보는 테스트는 @Transactional 을 쓰지 않는다 — 서비스가 연 트랜잭션이 실제로 커밋돼야
 * ro 쪽(다른 커넥션)에서 보인다. 그런 테스트가 끝나면 테이블을 비운다.
 * master(rw)만 쓰는 테스트는 예전처럼 @Transactional(테스트 트랜잭션, 끝나면 롤백)을 써도 된다.
 */
@SpringBootTest
abstract class ApplicationTestSupport {

    @MockitoBean lateinit var chatRoomPort: ChatRoomPort
    @MockitoBean lateinit var pushPort: PushPort
    @MockitoBean lateinit var storagePort: StoragePort
    @MockitoBean lateinit var profilePort: ProfilePort
    @MockitoBean lateinit var commercePort: CommercePort

    @Autowired @Qualifier("rwHikariDataSource") lateinit var rwHikariDataSource: HikariDataSource

    @AfterEach
    fun cleanDatabase() {
        // 테스트 트랜잭션 안이면 끝나고 롤백된다. 다른 커넥션으로 지우려 들면 잠금에 걸린다.
        if (TransactionSynchronizationManager.isActualTransactionActive()) return
        DbCleaner.clean(rwHikariDataSource)
    }
}
