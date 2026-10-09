package com.example.chatservice.application.service

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.example.chatservice.application.common.lock.ApiLockAop
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.support.ChatFixtures
import com.example.chatservice.application.usecase.result.CreatedChatRoom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * Redis 락(@ApiLock)을 끈 채로(테스트 설정 modu.api-lock.enabled=false) 같은 멤버 구성의 방 만들기를 동시에 보낸다.
 * `chat_room.member_key` 유니크 키만으로 방이 하나만 생기고 모두 같은 roomId 를 받는지 본다 —
 * Redis 장애로 1겹을 건너뛰어도 정확해야 한다.
 *
 * 매핑(@Table(uniqueConstraints = …))에서 유니크 제약을 빼면 이 테스트의 방 개수·roomId 단정이 깨진다(확인함).
 */
@SpringBootTest
class ChatRoomCreateConcurrencyTest {

    @Autowired lateinit var chatRoomCommandService: ChatRoomCommandService
    @Autowired lateinit var fixtures: ChatFixtures
    @Autowired lateinit var context: ApplicationContext
    @Autowired @Qualifier("rwHikariDataSource") lateinit var dataSource: DataSource
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

    private val jdbc by lazy { JdbcTemplate(dataSource) }
    private val serviceLogger = LoggerFactory.getLogger(ChatRoomCommandService::class.java) as Logger
    private val logs = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setUp() {
        fixtures.clear()
        logs.start()
        serviceLogger.addAppender(logs)
    }

    @AfterEach
    fun cleanUp() {
        serviceLogger.detachAppender(logs)
        logs.stop()
        fixtures.clear()
    }

    @Test
    fun redisLockIsOff_inThisTest() {
        assertThat(context.getBeansOfType(ApiLockAop::class.java)).isEmpty()
    }

    @Test
    fun concurrentCreates_forSameMembers_createExactlyOneRoom() {
        val threads = 8
        val pool = Executors.newFixedThreadPool(threads)
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        try {
            val futures = (0 until threads).map { i ->
                pool.submit<CreatedChatRoom> {
                    ready.countDown()
                    start.await()
                    // 양쪽이 서로 다른 순서로 보낸다(나 → 상대, 상대 → 나).
                    chatRoomCommandService.createOrGet(RoomMemberSet(if (i % 2 == 0) listOf(1L, 2L) else listOf(2L, 1L)))
                }
            }
            ready.await(10, TimeUnit.SECONDS)
            start.countDown()
            val results = futures.map { it.get(30, TimeUnit.SECONDS) }

            assertThat(results.map { it.room.roomId }.toSet()).hasSize(1)
            assertThat(results.count { it.created }).isEqualTo(1)
            assertThat(jdbc.queryForObject("select count(*) from chat_room", Long::class.java)).isEqualTo(1L)
            assertThat(jdbc.queryForObject("select count(*) from chat_room_member", Long::class.java)).isEqualTo(2L)
            // 만들 때 멤버 구성 지문이 들어갔다.
            assertThat(jdbc.queryForObject("select member_key from chat_room", String::class.java))
                .isEqualTo(RoomMemberSet(listOf(1L, 2L)).memberKey)
            // 진 요청들은 유니크 키에 막힌 뒤 이긴 방을 다시 찾아 돌려줬다(= 유니크 키가 실제로 막았다).
            assertThat(logs.list.count { it.formattedMessage.contains("lost the race") }).isGreaterThanOrEqualTo(1)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun differentMembers_stillGetTheirOwnRooms() {
        val first = chatRoomCommandService.createOrGet(RoomMemberSet(listOf(1L, 2L)))
        val second = chatRoomCommandService.createOrGet(RoomMemberSet(listOf(1L, 3L)))
        val again = chatRoomCommandService.createOrGet(RoomMemberSet(listOf(2L, 1L)))

        assertThat(first.created).isTrue()
        assertThat(second.created).isTrue()
        assertThat(second.room.roomId).isNotEqualTo(first.room.roomId)
        assertThat(again.created).isFalse()
        assertThat(again.room.roomId).isEqualTo(first.room.roomId)
    }

    @Test
    fun createOrGet_setsMemberKey_onTheNewRoom() {
        val members = RoomMemberSet(listOf(78L, 79L))

        val created = chatRoomCommandService.createOrGet(members)

        assertThat(created.created).isTrue()
        assertThat(memberKeyOf(created.room.roomId!!)).isEqualTo(members.memberKey)
    }

    /**
     * 유니크 키에 막혔는데 같은 구성의 방이 끝까지 안 보이면 삼키지 않고 그대로 던진다(한 번만 다시 찾는다).
     * 멤버 행 없이 지문만 남은 방을 심어 그 상황을 만든다.
     */
    @Test
    fun createOrGet_rethrows_whenTheKeyIsTakenButNoRoomWithThoseMembersExists() {
        val members = RoomMemberSet(listOf(1L, 2L))
        jdbc.update(
            "insert into chat_room(room_id, room_name, room_image, last_chat_msg, last_chat_id, last_chat_time, member_key) " +
                "values (?, ?, '', '', '', '2026-10-09 00:00:00', ?)",
            "orphan-key-room", "orphan", members.memberKey,
        )

        assertThatThrownBy { chatRoomCommandService.createOrGet(members) }
            .isInstanceOf(DataIntegrityViolationException::class.java)

        // 막힌 쓰기는 자기 트랜잭션만 되돌렸다 — 심어 둔 방 하나만 남는다.
        assertThat(jdbc.queryForObject("select count(*) from chat_room", Long::class.java)).isEqualTo(1L)
        assertThat(logs.list.count { it.formattedMessage.contains("member_key unique key") }).isEqualTo(1)
    }

    private fun memberKeyOf(roomId: String): String? =
        jdbc.queryForObject("select member_key from chat_room where room_id = ?", String::class.java, roomId)
}
