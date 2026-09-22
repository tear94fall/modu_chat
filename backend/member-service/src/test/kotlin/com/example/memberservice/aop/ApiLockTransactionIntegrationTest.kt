package com.example.memberservice.aop

import com.example.memberservice.global.lock.ApiLock
import com.example.memberservice.global.lock.LockParam
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * ApiLockAop(@Order(HIGHEST_PRECEDENCE)) -> AopForTransaction(REQUIRES_NEW) -> 대상 메서드
 * 순서로 실제 스프링 컨텍스트를 띄워 안쪽 트랜잭션이 정말로 열리는지, 그리고 락이 본문 실행
 * 동안에는 걸려 있다가 끝나면 풀리는지를 검증한다. MemberService.createMember 처럼 대상
 * 메서드에 전파 옵션(특히 NOT_SUPPORTED)을 다시 붙이면 첫 번째 테스트가 회귀를 잡아낸다.
 *
 * probe 는 CGLIB 프록시다. 프록시는 어드바이스가 적용된 호출을 별도의 타깃 인스턴스로
 * 위임하기 때문에, 본문에서 바뀐 상태는 프록시 인스턴스 자신의 필드가 아니라 getter 메서드
 * (역시 프록시를 거쳐 같은 타깃 인스턴스로 위임된다) 로 읽어야 한다. 그래서 클래스와 메서드가 open 이다.
 */
@SpringBootTest
@Import(ApiLockTransactionIntegrationTest.TestConfig::class)
class ApiLockTransactionIntegrationTest {

    @Autowired lateinit var probe: LockedProbe
    @Autowired lateinit var redissonClient: RedissonClient

    @Test
    fun innerTransaction_isActive_whileApiLockBodyRuns() {
        val result = probe.run("a")

        assertThat(result).isEqualTo("a")
        assertThat(probe.isTransactionActive()).isTrue()
    }

    @Test
    fun lock_isHeld_duringBody_andReleased_afterReturn() {
        probe.run("b")

        assertThat(probe.isLockedDuringBody()).isTrue()
        assertThat(redissonClient.getLock("LOCK:probe:b").isLocked).isFalse()
    }

    @TestConfiguration
    class TestConfig {

        @Bean
        fun lockedProbe(redissonClient: RedissonClient): LockedProbe = LockedProbe(redissonClient)
    }

    open class LockedProbe(private val redissonClient: RedissonClient) {

        private var transactionActive = false
        private var lockedDuringBody = false

        @ApiLock(prefix = "probe")
        open fun run(@LockParam key: String): String {
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive()
            lockedDuringBody = redissonClient.getLock("LOCK:probe:$key").isLocked
            return key
        }

        open fun isTransactionActive(): Boolean = transactionActive

        open fun isLockedDuringBody(): Boolean = lockedDuringBody
    }
}
