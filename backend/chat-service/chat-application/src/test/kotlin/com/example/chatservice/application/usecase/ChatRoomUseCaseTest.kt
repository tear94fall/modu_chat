package com.example.chatservice.application.usecase

import com.example.chatservice.application.common.exception.ErrorCode
import com.example.chatservice.application.domain.entity.ReactionEmoji
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.BlockedIdsCache
import com.example.chatservice.application.member.MemberGateway
import com.example.chatservice.application.member.MemberInfo
import com.example.chatservice.application.support.ChatFixtures
import com.example.chatservice.application.support.assertErrorCode
import com.example.chatservice.application.support.stubMembers
import com.example.chatservice.application.support.userId
import com.example.chatservice.application.usecase.command.UpdateChatRoomCommand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/** 방 유스케이스를 H2 위에서 돌린다. 바깥(member-service, Kafka)만 목이다. */
@SpringBootTest
class ChatRoomUseCaseTest {

    @Autowired lateinit var chatRoomUseCase: ChatRoomUseCase
    @Autowired lateinit var fixtures: ChatFixtures
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher
    @MockitoBean lateinit var blockedIdsCache: BlockedIdsCache

    @BeforeEach
    fun setUp() {
        fixtures.clear()
        memberGateway.stubMembers()
        whenever(blockedIdsCache.get(anyOrNull())).thenReturn(setOf())
    }

    @AfterEach
    fun cleanUp() = fixtures.clear()

    // ---------- 방 만들기 ----------

    @Test
    @DisplayName("방을 만들면 topic-chat-room-created 로 roomId 를 발행한다")
    fun create_publishesRoomCreatedEvent() {
        val view = chatRoomUseCase.create(userId(1), listOf(1L, 2L))

        assertThat(view.room.roomId).isNotBlank()
        verify(chatEventPublisher).roomCreated(view.room.roomId!!)
        // 발행은 커밋 뒤다: ws-service 가 이벤트를 받자마자 방을 조회해도 방이 있다.
        assertThat(fixtures.memberIdsOf(view.room.roomId!!)).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    @DisplayName("멤버를 하나도 못 찾으면 방을 만들지 않고 발행도 하지 않는다")
    fun create_withNoMembers_doesNotPublish() {
        whenever(memberGateway.byIds(any())).thenReturn(listOf())

        assertErrorCode(ErrorCode.USERID_NOT_FOUND_ERROR) { chatRoomUseCase.create(userId(1), listOf(1L, 999L)) }

        verify(chatEventPublisher, never()).roomCreated(any())
    }

    @Test
    @DisplayName("같은 멤버 구성의 방이 있으면 새로 만들지 않고 그 방을 돌려준다")
    fun create_withSameMemberSet_returnsExistingRoom() {
        fixtures.room("room-existing", 1L, 2L)

        val view = chatRoomUseCase.create(userId(1), listOf(2L, 1L))

        assertThat(view.room.roomId).isEqualTo("room-existing")
        assertThat(view.members).extracting<Long> { it.id }.containsExactlyInAnyOrder(1L, 2L)
        verify(chatEventPublisher, never()).roomCreated(any())
    }

    @Test
    @DisplayName("같은 멤버 구성의 방이 없으면 새 방을 만들고 발행한다")
    fun create_withoutMatchingRoom_createsNewRoom() {
        fixtures.room("room-group", 1L, 2L, 3L)

        val view = chatRoomUseCase.create(userId(1), listOf(1L, 2L))

        assertThat(view.room.roomId).isNotEqualTo("room-group")
        assertThat(view.room.roomName).isEqualTo("새로운 채팅방")
        // 새 방 응답의 members 는 지금까지처럼 비어 있다(앱은 roomId 만 쓰고 방 목록을 다시 읽는다).
        assertThat(view.members).isEmpty()
        verify(chatEventPublisher).roomCreated(view.room.roomId!!)
    }

    @Test
    @DisplayName("요청 id 에 중복이 있어도 멤버는 한 번만 등록한다")
    fun create_withDuplicateIds_registersEachMemberOnce() {
        val view = chatRoomUseCase.create(userId(1), listOf(1L, 1L, 2L))

        val ids = argumentCaptor<List<Long>>()
        verify(memberGateway).byIds(ids.capture())
        assertThat(ids.firstValue).containsExactly(1L, 2L)
        assertThat(fixtures.memberIdsOf(view.room.roomId!!)).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    @DisplayName("방 멤버 목록에 나 자신이 없으면 만들 수 없다(403)")
    fun create_withoutMyself_isForbidden() {
        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.create(userId(1), listOf(2L, 3L)) }

        verify(chatEventPublisher, never()).roomCreated(any())
    }

    @Test
    @DisplayName("이벤트 발행이 실패해도 방 만들기는 성공이다")
    fun create_survivesPublishFailure() {
        doThrow(RuntimeException("kafka down")).whenever(chatEventPublisher).roomCreated(any())

        val view = chatRoomUseCase.create(userId(1), listOf(1L, 2L))

        assertThat(fixtures.roomExists(view.room.roomId!!)).isTrue()
    }

    // ---------- 초대 ----------

    @Test
    @DisplayName("초대하면 member-service 가 돌려준 회원을 한 번씩만 추가하고, 이미 있는 회원은 건너뛴다")
    fun invite_addsInvitedOnce_andSkipsExistingMembers() {
        val room = fixtures.room("room-1", 1L)

        // member-service 는 실제 초대된 회원 목록을 돌려준다(이미 있는 1 도 같이 돌려주는 상황).
        val view = chatRoomUseCase.invite(userId(1), "room-1", listOf(userId(1), userId(2)))

        assertThat(fixtures.memberIdsOf("room-1")).containsExactlyInAnyOrder(1L, 2L)
        assertThat(view.room.memberIds).containsExactlyInAnyOrder(1L, 2L)
        verify(memberGateway, times(1)).invite(eq(room.id), any())
    }

    @Test
    @DisplayName("방 멤버가 아니면 초대할 수 없다(403)")
    fun invite_byOutsider_isForbidden() {
        fixtures.room("room-1", 1L, 2L)

        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatRoomUseCase.invite(userId(3), "room-1", listOf(userId(3))) }

        assertThat(fixtures.memberIdsOf("room-1")).containsExactlyInAnyOrder(1L, 2L)
        verify(memberGateway, never()).invite(anyOrNull(), any())
    }

    @Test
    @DisplayName("초대받은 사람은 바로 방 정보와 멤버를 볼 수 있다")
    fun invited_canReadRoomRightAway() {
        fixtures.room("room-1", 1L, 2L)
        chatRoomUseCase.invite(userId(1), "room-1", listOf(userId(3)))

        val view = chatRoomUseCase.room(userId(3), "room-1")

        assertThat(view.members).extracting<String> { it.userId }.containsExactlyInAnyOrder("user-1", "user-2", "user-3")
    }

    // ---------- 조회 ----------

    @Test
    @DisplayName("속한 방이 없으면 member-service 의 회원 조회를 하지 않고 빈 목록을 돌려준다")
    fun roomsOf_withNoRooms_returnsEmptyWithoutMemberLookup() {
        val rooms = chatRoomUseCase.roomsOf(userId(8), "8")

        assertThat(rooms).isEmpty()
        verify(memberGateway, never()).byIds(any())
    }

    @Test
    @DisplayName("방 목록은 내 회원 id 로도, 내 userId 로도 읽을 수 있고 남의 id 로는 못 읽는다(403)")
    fun roomsOf_onlyMyself() {
        fixtures.room("room-1", 1L, 2L)
        fixtures.room("room-2", 2L, 3L)

        assertThat(chatRoomUseCase.roomsOf(userId(1), "1")).extracting<String> { it.room.roomId }.containsExactly("room-1")
        assertThat(chatRoomUseCase.roomsOf(userId(1), userId(1))).extracting<String> { it.room.roomId }.containsExactly("room-1")
        assertThat(chatRoomUseCase.roomsOf(userId(1), "1")[0].members).extracting<Long> { it.id }.containsExactlyInAnyOrder(1L, 2L)
        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.roomsOf(userId(1), "2") }
        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.roomsOf(userId(1), userId(2)) }
        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.roomsOf(null, "1") }
    }

    @Test
    @DisplayName("방 정보·읽음 커서·수정은 방 멤버만 할 수 있고(403), 없는 방은 404 다")
    fun room_onlyForMembers() {
        fixtures.room("room-1", 1L, 2L)
        val command = UpdateChatRoomCommand("이름", "", "", "", "2026-09-13 00:00:00")

        assertThat(chatRoomUseCase.room(userId(1), "room-1").room.roomId).isEqualTo("room-1")
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatRoomUseCase.room(userId(3), "room-1") }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatRoomUseCase.readCursors(userId(3), "room-1") }
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatRoomUseCase.update(userId(3), "room-1", command) }
        assertErrorCode(ErrorCode.CHATROOM_NOT_FOUND_ERROR) { chatRoomUseCase.room(userId(1), "no-room") }
        // 내부 호출(ws-service)은 멤버 확인 없이 읽는다.
        assertThat(chatRoomUseCase.roomForInternal("room-1").members).hasSize(2)
    }

    @Test
    @DisplayName("1:1 방 찾기는 나와 상대 둘만 있는 방만 돌려주고, 남의 userId 로는 못 찾는다(403)")
    fun oneOnOneRooms_findsExactPair() {
        fixtures.room("room-pair", 1L, 2L)
        fixtures.room("room-group", 1L, 2L, 3L)

        assertThat(chatRoomUseCase.oneOnOneRooms(userId(1), userId(1), userId(2)))
            .extracting<String> { it.room.roomId }.containsExactly("room-pair")
        assertThat(chatRoomUseCase.oneOnOneRooms(userId(1), userId(1), userId(3))).isEmpty()
        assertThat(chatRoomUseCase.oneOnOneRooms(userId(1), userId(1), "nobody")).isEmpty()
        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.oneOnOneRooms(userId(3), userId(1), userId(2)) }
    }

    @Test
    @DisplayName("빠진 값이 있는 방 수정은 400 이다")
    fun update_requiresEveryField() {
        assertThatThrownBy { UpdateChatRoomCommand("이름", null, "", "", "") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    // ---------- 방 나가기 (고친 버그) ----------

    @Test
    @DisplayName("방을 나가면 내 멤버 행이 지워지고 member-service 의 방 목록에서도 빠진다")
    fun leave_removesMembershipRow_andSyncsMemberService() {
        val room = fixtures.room("room-1", 1L, 2L, 3L)

        val view = chatRoomUseCase.leave(userId(1), "room-1", userId(1))

        assertThat(fixtures.memberIdsOf("room-1")).containsExactlyInAnyOrder(2L, 3L)
        assertThat(view.room.roomId).isEqualTo("room-1")
        assertThat(view.members).isEmpty()
        val exited = argumentCaptor<List<MemberInfo>>()
        verify(memberGateway).exit(eq(room.id), exited.capture())
        assertThat(exited.firstValue).extracting<Long> { it.id }.containsExactly(1L)
        // 나간 방은 내 방 목록에 없고, 더는 볼 수도 없다.
        assertThat(chatRoomUseCase.roomsOf(userId(1), "1")).isEmpty()
        assertErrorCode(ErrorCode.NOT_CHAT_ROOM_MEMBER) { chatRoomUseCase.room(userId(1), "room-1") }
    }

    @Test
    @DisplayName("마지막 사람이 나가 비게 된 방은 대화·반응째 지운다")
    fun leave_deletesRoomLeftEmpty() {
        val room = fixtures.room("room-alone", 1L)
        val chat = fixtures.chat(room, userId(1), "혼잣말")
        fixtures.reaction(chat, userId(2), ReactionEmoji.LIKE)

        chatRoomUseCase.leave(userId(1), "room-alone", userId(1))

        assertThat(fixtures.roomExists("room-alone")).isFalse()
        assertErrorCode(ErrorCode.CHATROOM_NOT_FOUND_ERROR) { chatRoomUseCase.leave(userId(1), "room-alone", userId(1)) }
    }

    @Test
    @DisplayName("남을 내보낼 수 없고(403), 이미 나간 방을 또 나가도 오류가 아니다")
    fun leave_onlyMyself_andIdempotent() {
        fixtures.room("room-1", 1L, 2L)

        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.leave(userId(1), "room-1", userId(2)) }
        assertThat(fixtures.memberIdsOf("room-1")).containsExactlyInAnyOrder(1L, 2L)

        chatRoomUseCase.leave(userId(1), "room-1", userId(1))
        chatRoomUseCase.leave(userId(1), "room-1", userId(1))

        assertThat(fixtures.memberIdsOf("room-1")).containsExactly(2L)
    }

    @Test
    @DisplayName("member-service 반영이 실패해도 나가기는 끝난다")
    fun leave_survivesMemberServiceFailure() {
        fixtures.room("room-1", 1L, 2L)
        doThrow(RuntimeException("member-service down")).whenever(memberGateway).exit(anyOrNull(), any())

        chatRoomUseCase.leave(userId(1), "room-1", userId(1))

        assertThat(fixtures.memberIdsOf("room-1")).containsExactly(2L)
    }

    // ---------- 탈퇴 정리 ----------

    @Test
    fun exitAll_removesMemberships_andDeletesRoomsLeftEmpty() {
        val shared = fixtures.room("같이 있는 방", 7L, 8L)
        val alone = fixtures.room("혼자 남은 방", 7L)

        val left = chatRoomUseCase.exitAll(7L)

        // 두 방 모두에서 내 멤버 행이 지워진다.
        assertThat(fixtures.memberIdsOf("같이 있는 방")).containsExactly(8L)
        // 남는 사람이 있는 방은 그대로, 비게 된 방은 지운다.
        assertThat(fixtures.roomExists("같이 있는 방")).isTrue()
        assertThat(fixtures.roomExists("혼자 남은 방")).isFalse()
        assertThat(left).containsExactlyInAnyOrder(shared.id, alone.id)
        // member-service 가 부른 것이라 member-service 를 다시 부르지 않는다.
        verify(memberGateway, never()).exit(anyOrNull(), any())
    }

    @Test
    fun exitAll_withNoRooms_doesNothing() {
        fixtures.room("room-1", 1L, 2L)

        assertThat(chatRoomUseCase.exitAll(9L)).isEmpty()
        assertThat(fixtures.memberIdsOf("room-1")).containsExactlyInAnyOrder(1L, 2L)
    }

    // ---------- 읽음 ----------

    @Test
    @DisplayName("읽음 처리는 나만 할 수 있고, 내부 호출은 회원 id 와 userId 를 둘 다 받는다")
    fun markRead_movesCursorToLastChat() {
        fixtures.room("room-1", 1L, 2L, lastChatId = "10", lastReadChatId = "5")

        chatRoomUseCase.markRead(userId(1), "room-1", "1")
        assertThat(cursors("room-1")).containsEntry("user-1", 10L).containsEntry("user-2", 5L)

        assertErrorCode(ErrorCode.FORBIDDEN) { chatRoomUseCase.markRead(userId(1), "room-1", "2") }
        assertErrorCode(ErrorCode.CHATROOM_NOT_FOUND_ERROR) { chatRoomUseCase.markRead(userId(1), "no-room", "1") }
        assertErrorCode(ErrorCode.USERID_NOT_FOUND) { chatRoomUseCase.markRead(userId(3), "room-1", userId(3)) }

        chatRoomUseCase.markReadForInternal("room-1", userId(2))
        assertThat(cursors("room-1")).containsEntry("user-2", 10L)
        assertErrorCode(ErrorCode.USERID_NOT_FOUND) { chatRoomUseCase.markReadForInternal("room-1", userId(3)) }
    }

    @Test
    @DisplayName("member-service 가 응답하지 않으면 읽음 커서는 빈 목록이다")
    fun readCursors_withoutMemberService_isEmpty() {
        fixtures.room("room-1", 1L, 2L)
        chatRoomUseCase.room(userId(1), "room-1")
        whenever(memberGateway.byIds(any())).thenThrow(RuntimeException("member-service down"))

        assertThat(chatRoomUseCase.readCursors(userId(1), "room-1")).isEmpty()
    }

    private fun cursors(roomId: String): Map<String, Long> =
        chatRoomUseCase.readCursors(userId(1), roomId).associate { it.userId to it.lastReadChatId }
}
