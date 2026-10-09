package com.example.chatservice.application.service

import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.support.ChatFixtures
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * `chat_room.member_key` 는 만들 때의 멤버 구성 지문이다. 멤버가 바뀌면(초대·나가기·탈퇴) null 이 되어야 한다.
 * 그러지 않으면 구성이 바뀐 옛 방의 지문이 남아, 뒤에 원래 구성으로 방을 만들려는 사람을 유니크 키가 막아 버린다.
 */
@SpringBootTest
class ChatRoomMemberKeyTest {

    @Autowired lateinit var chatRoomCommandService: ChatRoomCommandService
    @Autowired lateinit var fixtures: ChatFixtures
    @Autowired @Qualifier("rwHikariDataSource") lateinit var dataSource: DataSource
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

    private val jdbc by lazy { JdbcTemplate(dataSource) }

    @BeforeEach
    fun setUp() = fixtures.clear()

    @AfterEach
    fun cleanUp() = fixtures.clear()

    @Test
    fun addMembers_clearsTheKey() {
        val members = RoomMemberSet(listOf(1L, 2L))
        val roomId = chatRoomCommandService.createOrGet(members).room.roomId!!
        assertThat(memberKeyOf(roomId)).isEqualTo(members.memberKey)

        chatRoomCommandService.addMembers(roomId, listOf(3L))

        assertThat(memberKeyOf(roomId)).isNull()
    }

    @Test
    fun addMembers_keepsTheKey_whenNobodyWasActuallyAdded() {
        val members = RoomMemberSet(listOf(1L, 2L))
        val roomId = chatRoomCommandService.createOrGet(members).room.roomId!!

        // 이미 방에 있는 사람만 넘긴다 — 멤버 구성이 바뀌지 않았으니 지문도 그대로다.
        chatRoomCommandService.addMembers(roomId, listOf(1L, 2L))

        assertThat(memberKeyOf(roomId)).isEqualTo(members.memberKey)
    }

    @Test
    fun removeMember_clearsTheKey_ofTheSurvivingRoom() {
        val roomId = chatRoomCommandService.createOrGet(RoomMemberSet(listOf(1L, 2L, 3L))).room.roomId!!

        chatRoomCommandService.removeMember(roomId, 3L)

        assertThat(memberKeyOf(roomId)).isNull()
        assertThat(fixtures.memberIdsOf(roomId)).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    fun exitAll_clearsTheKey_ofTheSurvivingRoom() {
        val roomId = chatRoomCommandService.createOrGet(RoomMemberSet(listOf(1L, 2L, 3L))).room.roomId!!

        chatRoomCommandService.exitAll(3L)

        assertThat(memberKeyOf(roomId)).isNull()
        assertThat(fixtures.memberIdsOf(roomId)).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    fun theOriginalMemberSet_canMakeANewRoom_afterSomeoneLeft() {
        val members = RoomMemberSet(listOf(1L, 2L))
        val oldRoomId = chatRoomCommandService.createOrGet(members).room.roomId!!
        chatRoomCommandService.removeMember(oldRoomId, 2L)
        assertThat(memberKeyOf(oldRoomId)).isNull()

        // 같은 지문으로 다시 만든다. 옛 방의 지문이 null 이라(유니크 인덱스는 null 을 세지 않는다) 막히지 않는다.
        val created = chatRoomCommandService.createOrGet(members)

        assertThat(created.created).isTrue()
        assertThat(created.room.roomId).isNotEqualTo(oldRoomId)
        assertThat(memberKeyOf(created.room.roomId!!)).isEqualTo(members.memberKey)
        assertThat(jdbc.queryForObject("select count(*) from chat_room", Long::class.java)).isEqualTo(2L)
    }

    @Test
    fun invitingBack_doesNotRestoreTheKey() {
        val roomId = chatRoomCommandService.createOrGet(RoomMemberSet(listOf(1L, 2L, 3L))).room.roomId!!
        chatRoomCommandService.removeMember(roomId, 3L)

        chatRoomCommandService.addMembers(roomId, listOf(3L))

        // 멤버 구성은 원래대로지만 지문은 돌아오지 않는다. 방 찾기는 멤버 목록 조회가 하므로 동작은 그대로다.
        assertThat(memberKeyOf(roomId)).isNull()
        assertThat(chatRoomCommandService.createOrGet(RoomMemberSet(listOf(1L, 2L, 3L))).room.roomId).isEqualTo(roomId)
    }

    private fun memberKeyOf(roomId: String): String? =
        jdbc.queryForObject("select member_key from chat_room where room_id = ?", String::class.java, roomId)
}
