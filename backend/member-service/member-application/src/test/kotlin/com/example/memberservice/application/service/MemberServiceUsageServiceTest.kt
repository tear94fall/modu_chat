package com.example.memberservice.application.service

import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.config.UsageProperties
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.repository.rw.MemberFriendRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberServiceUsageRwRepository
import com.example.memberservice.application.usecase.command.BulkUsageCommand
import com.example.memberservice.application.usecase.command.RecordUsageCommand
import com.example.memberservice.application.support.ApplicationTestSupport
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * 서비스 이용 기록: 기록·1시간 제한·모르는 클라이언트·백필(bulk, 기동 때 채팅). 시계는 테스트가 움직인다.
 * 다른 테스트가 남긴 회원과 섞이지 않게 userId 에 표식을 붙여 좁혀서 본다.
 * 서비스를 직접 만들어(프록시 없음) 테스트 트랜잭션 안에서 돌린다 — 이용 기록 쓰기는 master(rw)만 쓴다.
 */
@Transactional
class MemberServiceUsageServiceTest : ApplicationTestSupport() {

    companion object {
        private const val MARK = "usagecase"
        private val START: Instant = Instant.parse("2026-09-28T00:00:00Z")
    }

    /** 테스트가 앞으로 돌릴 수 있는 UTC 시계. */
    class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
        fun advance(d: Duration) { now = now.plus(d) }
    }

    @Autowired lateinit var usageRepository: MemberServiceUsageRwRepository
    @Autowired lateinit var memberRepository: MemberRwRepository
    @Autowired lateinit var memberFriendRepository: MemberFriendRwRepository
    @Autowired lateinit var properties: UsageProperties

    private lateinit var clock: MutableClock
    private lateinit var service: MemberUsageCommandService

    @BeforeEach
    fun setUp() {
        clock = MutableClock(START)
        service = MemberUsageCommandService(usageRepository, memberRepository, properties, clock)
    }

    private fun member(id: String, chatRooms: MutableList<Long> = mutableListOf()): Member = memberRepository.save(
        Member(userId = "$MARK-$id", email = "$MARK-$id@example.com", username = id, profiles = mutableListOf(), chatRoomMembers = chatRooms),
    )

    private fun at(instant: Instant): LocalDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    @Test
    fun 설정의_실제_클라이언트_ID_를_서비스로_바꾼다() {
        assertThat(properties.usageClients).containsEntry("modu-chat", ModuService.CHAT)
            .containsEntry("modu-commerce", ModuService.COMMERCE)
            .doesNotContainKey("modu-admin")
    }

    @Test
    fun 처음_이용하면_처음과_마지막이_같은_기록을_만든다() {
        val m = member("first")

        service.record(RecordUsageCommand(m.userId, "modu-chat"))

        val row = usageRepository.findByUserIdAndService(m.userId, ModuService.CHAT)!!
        assertThat(row.firstUsedAt).isEqualTo(at(START))
        assertThat(row.lastUsedAt).isEqualTo(at(START))
        assertThat(usageRepository.findAllByUserId(m.userId)).hasSize(1)
    }

    @Test
    fun 한_시간_안에는_마지막_이용_시각을_바꾸지_않고_지나면_바꾼다() {
        val m = member("throttle")
        service.record(RecordUsageCommand(m.userId, "modu-commerce"))

        clock.advance(Duration.ofMinutes(59))
        service.record(RecordUsageCommand(m.userId, "modu-commerce"))
        assertThat(usageRepository.findByUserIdAndService(m.userId, ModuService.COMMERCE)!!.lastUsedAt).isEqualTo(at(START))

        clock.advance(Duration.ofMinutes(2))
        service.record(RecordUsageCommand(m.userId, "modu-commerce"))
        val row = usageRepository.findByUserIdAndService(m.userId, ModuService.COMMERCE)!!
        assertThat(row.firstUsedAt).isEqualTo(at(START))
        assertThat(row.lastUsedAt).isEqualTo(at(START.plus(Duration.ofMinutes(61))))
        assertThat(usageRepository.findAllByUserId(m.userId)).hasSize(1)
    }

    @Test
    fun 모르는_클라이언트나_모르는_회원은_기록하지_않는다() {
        val m = member("ignored")

        service.record(RecordUsageCommand(m.userId, "modu-admin"))
        service.record(RecordUsageCommand(m.userId, "nope"))
        service.record(RecordUsageCommand(m.userId, null))
        service.record(RecordUsageCommand("$MARK-no-such-user", "modu-chat"))

        assertThat(usageRepository.findAllByUserId(m.userId)).isEmpty()
        assertThat(usageRepository.findAllByUserId("$MARK-no-such-user")).isEmpty()
    }

    @Test
    fun bulk_는_없는_기록만_넣고_회원이_아닌_ID_는_건너뛴다() {
        val a = member("bulk-a")
        val b = member("bulk-b")
        service.record(RecordUsageCommand(a.userId, "modu-commerce"))

        val inserted = service.bulkInsert(
            BulkUsageCommand(
                ModuService.COMMERCE,
                listOf(a.userId, b.userId, b.userId, "$MARK-bulk-ghost"),
                "2026-01-02T03:04:05",
            ),
        )

        assertThat(inserted).isEqualTo(1)
        val rowB = usageRepository.findByUserIdAndService(b.userId, ModuService.COMMERCE)!!
        assertThat(rowB.firstUsedAt).isEqualTo(LocalDateTime.parse("2026-01-02T03:04:05"))
        assertThat(rowB.lastUsedAt).isEqualTo(rowB.firstUsedAt)
        // 이미 있던 기록은 그대로다.
        assertThat(usageRepository.findByUserIdAndService(a.userId, ModuService.COMMERCE)!!.firstUsedAt).isEqualTo(at(START))
        assertThat(usageRepository.findAllByUserId("$MARK-bulk-ghost")).isEmpty()
        // 두 번째는 넣을 것이 없다.
        assertThat(service.bulkInsert(BulkUsageCommand(ModuService.COMMERCE, listOf(a.userId, b.userId), null))).isEqualTo(0)
    }

    @Test
    fun bulk_의_usedAt_이_없으면_지금이고_오프셋이_있으면_UTC_로_바꾼다() {
        val a = member("bulk-now")
        val b = member("bulk-offset")

        service.bulkInsert(BulkUsageCommand(ModuService.CHAT, listOf(a.userId), null))
        service.bulkInsert(BulkUsageCommand(ModuService.CHAT, listOf(b.userId), "2026-01-02T09:00:00+09:00"))

        assertThat(usageRepository.findByUserIdAndService(a.userId, ModuService.CHAT)!!.firstUsedAt).isEqualTo(at(START))
        assertThat(usageRepository.findByUserIdAndService(b.userId, ModuService.CHAT)!!.firstUsedAt)
            .isEqualTo(LocalDateTime.parse("2026-01-02T00:00:00"))
    }

    @Test
    fun bulk_는_천_개를_넘거나_service_가_없으면_거부한다() {
        assertThatThrownBy { service.bulkInsert(BulkUsageCommand(ModuService.CHAT, (0..1000).map { "$MARK-$it" }, null)) }
            .isInstanceOf(ResponseStatusException::class.java)
        assertThatThrownBy { service.bulkInsert(BulkUsageCommand(null, listOf("x"), null)) }
            .isInstanceOf(ResponseStatusException::class.java)
        assertThatThrownBy { service.bulkInsert(BulkUsageCommand(ModuService.CHAT, listOf("x"), "yesterday")) }
            .isInstanceOf(ResponseStatusException::class.java)
    }

    @Test
    fun 채팅_백필은_채팅방이나_친구가_있는_활동_회원만_가입_시각으로_넣고_여러_번_돌려도_같다() {
        val inRoom = member("bf-room", mutableListOf(10L))
        val hasFriend = member("bf-friend")
        val befriended = member("bf-befriended")
        val lonely = member("bf-lonely")
        val withdrawn = member("bf-withdrawn", mutableListOf(11L))
        val already = member("bf-already", mutableListOf(12L))
        memberFriendRepository.save(MemberFriend.of(hasFriend, befriended))
        withdrawn.withdraw()
        withdrawn.addChatRoom(11L) // 탈퇴하면 방이 비워지지만, 남아 있어도 넣지 않는다.
        service.record(RecordUsageCommand(already.userId, "modu-chat"))
        memberRepository.flush()

        val first = service.backfillChat()

        fun chat(m: Member) = usageRepository.findByUserIdAndService(m.userId, ModuService.CHAT)
        assertThat(chat(inRoom)!!.firstUsedAt).isEqualTo(inRoom.createdDate)
        assertThat(chat(inRoom)!!.lastUsedAt).isEqualTo(inRoom.createdDate)
        assertThat(chat(hasFriend)).isNotNull
        assertThat(chat(befriended)).isNotNull
        assertThat(chat(lonely)).isNull()
        assertThat(chat(withdrawn)).isNull()
        assertThat(chat(already)!!.firstUsedAt).isEqualTo(at(START))
        assertThat(first).isGreaterThanOrEqualTo(3)

        assertThat(service.backfillChat()).isEqualTo(0)
        assertThat(usageRepository.findAllByUserId(inRoom.userId)).hasSize(1)
    }
}
