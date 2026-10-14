package com.example.chatservice.application.common.lock

import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.service.ChatRoomCommandService
import com.example.chatservice.application.service.RoomMemberSet
import com.example.chatservice.application.support.ChatFixtures
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Redis 락을 켠 컨텍스트(Redisson 은 목)에서 방 만들기가 @ApiLock 을 거치는지, 키·대기 시간이 맞는지 본다.
 * 애스펙트는 Redis 락만 걸고 트랜잭션은 열지 않는다 — 방 만들기는 스스로 짧은 트랜잭션을 연다.
 */
@SpringBootTest(properties = ["${ApiLockAop.ENABLED_PROPERTY}=true"])
class ApiLockWiringTest {

    @Autowired lateinit var chatRoomCommandService: ChatRoomCommandService
    @Autowired lateinit var fixtures: ChatFixtures
    @MockitoBean lateinit var redissonClient: RedissonClient
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

    private val rLock: RLock = mock()

    @BeforeEach
    fun setUp() {
        fixtures.clear()
        whenever(redissonClient.getLock(EXPECTED_KEY)).thenReturn(rLock)
    }

    @AfterEach
    fun cleanUp() = fixtures.clear()

    @Test
    fun createOrGet_runsInsideRedisLock_keyedBySortedMembers() {
        var transactionActiveWhileLocked = false
        whenever(rLock.tryLock(10L, TimeUnit.SECONDS)).thenAnswer {
            transactionActiveWhileLocked = TransactionSynchronizationManager.isActualTransactionActive()
            true
        }
        whenever(rLock.isHeldByCurrentThread).thenReturn(true)

        val result = chatRoomCommandService.createOrGet(RoomMemberSet(listOf(2L, 1L)))

        assertThat(result.created).isTrue()
        // 락은 트랜잭션보다 먼저 잡힌다(HIGHEST_PRECEDENCE) — 잡을 때는 아직 트랜잭션이 없다.
        assertThat(transactionActiveWhileLocked).isFalse()
        verify(redissonClient).getLock(EXPECTED_KEY)
        verify(rLock).tryLock(10L, TimeUnit.SECONDS)
        verify(rLock).unlock()
    }

    @Test
    fun createOrGet_throwsBusy_whenRedisLockIsNotAcquiredInTime() {
        whenever(rLock.tryLock(10L, TimeUnit.SECONDS)).thenReturn(false)

        assertThatThrownBy { chatRoomCommandService.createOrGet(RoomMemberSet(listOf(1L, 2L))) }
            .isInstanceOf(ApiLockAcquisitionException::class.java)
            .hasMessageContaining(EXPECTED_KEY)
        verify(rLock, never()).unlock()
        assertThat(chatRoomCommandService.findRoomsOfMember(1L)).isEmpty()
    }

    @Test
    fun createOrGet_runsOutsideAnyTransaction_andIsKeyedByTheMemberSet() {
        // NOT_SUPPORTED 여야 클래스에 붙은 @Transactional 이 꺼진다. 트랜잭션 안에서 돌면 유니크 키 충돌로
        // 되돌아간 쓰기가 바깥 트랜잭션까지 못 쓰게 만들고, 재확인도 같은 스냅샷을 다시 읽는다.
        val method = ChatRoomCommandService::class.java.getMethod("createOrGet", RoomMemberSet::class.java)
        val tx = method.getAnnotation(Transactional::class.java)
        assertThat(tx.propagation).isEqualTo(Propagation.NOT_SUPPORTED)
        assertThat(method.getAnnotation(ApiLock::class.java).prefix).isEqualTo(RoomMemberSet.LOCK_PREFIX)
        assertThat(method.getAnnotation(ApiLock::class.java).waitTime).isEqualTo(10L)
        assertThat(method.getAnnotation(ApiLock::class.java).timeUnit).isEqualTo(TimeUnit.SECONDS)
        assertThat(method.parameters[0].getAnnotation(LockParam::class.java)).isNotNull()
    }

    @Test
    fun theApiLockAspect_doesNotOpenATransaction() {
        var transactionActiveInsideLock = false
        whenever(rLock.tryLock(10L, TimeUnit.SECONDS)).thenReturn(true)
        whenever(rLock.isHeldByCurrentThread).thenReturn(true)
        val joinPoint: ProceedingJoinPoint = mock()
        val signature: MethodSignature = mock()
        whenever(joinPoint.signature).thenReturn(signature)
        whenever(signature.method)
            .thenReturn(ChatRoomCommandService::class.java.getMethod("createOrGet", RoomMemberSet::class.java))
        whenever(joinPoint.args).thenReturn(arrayOf(RoomMemberSet(listOf(1L, 2L))))
        whenever(joinPoint.proceed()).thenAnswer {
            transactionActiveInsideLock = TransactionSynchronizationManager.isActualTransactionActive()
            null
        }

        ApiLockAop(redissonClient).lock(joinPoint)

        assertThat(transactionActiveInsideLock).isFalse()
    }

    private companion object {
        const val EXPECTED_KEY = "LOCK:chat:room-create:1,2"
    }
}
