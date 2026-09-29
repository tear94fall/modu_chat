package com.example.chatservice.api.pub

import com.example.chatservice.api.member.MemberFeignClient
import com.example.chatservice.api.support.ChatTables
import com.example.chatservice.api.support.stubMembers
import com.example.chatservice.application.event.ChatEventPublisher
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 앱 API 를 HTTP 로: 고친 버그 넷(단건 조회·방 나가기·삭제 권한·오류 응답)과 본인 확인. */
@SpringBootTest
@AutoConfigureMockMvc
class ChatPublicApiTest {

    companion object {
        private const val ROOM = "3f2b9c1e-8a7d-4e21-9b0a-2c6d5e4f1a3b"
        private const val GROUP = "7a1d2c3e-0000-4e21-9b0a-2c6d5e4f1a3b"
    }

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var memberFeignClient: MemberFeignClient
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

    private lateinit var tables: ChatTables
    private var mine = 0L
    private var theirs = 0L

    @Autowired
    fun dataSource(@Qualifier("rwHikariDataSource") dataSource: DataSource) {
        tables = ChatTables(JdbcTemplate(dataSource))
    }

    @BeforeEach
    fun setUp() {
        memberFeignClient.stubMembers()
        tables.clear()
        tables.room(ROOM, "둘이서", "", 1, 2)
        tables.room(GROUP, "셋이서", "", 1, 2, 3)
        mine = tables.chat(ROOM, "user-1", "내가 보낸 메시지", 1, "2026-09-20 09:00:01")
        theirs = tables.chat(ROOM, "user-2", "상대가 보낸 메시지", 1, "2026-09-20 09:00:02")
        tables.reaction(theirs, ROOM, "user-1", "LIKE", "2026-09-20 09:01:00")
        tables.setLastChat(ROOM, theirs)
    }

    @AfterEach
    fun cleanUp() = tables.clear()

    private fun call(builder: MockHttpServletRequestBuilder, user: Long? = 1): ResultActions {
        if (user != null) builder.header("X-Auth-User-Id", "user-$user")
        return mockMvc.perform(builder)
    }

    // ---------- 버그 1: 방의 메시지 단건 조회 ----------

    @Test
    fun chatInRoom_returnsTheMessage() {
        call(get("/api-public/chat/$ROOM/$theirs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(theirs))
            .andExpect(jsonPath("$.roomId").value(ROOM))
            .andExpect(jsonPath("$.message").value("상대가 보낸 메시지"))
            .andExpect(jsonPath("$.reactions[0].emoji").value("LIKE"))
        // 그 방의 메시지가 아니면 지금까지의 설명대로 빈 본문(200)이다.
        call(get("/api-public/chat/$GROUP/$theirs")).andExpect(status().isOk).andExpect(content().string(""))
        call(get("/api-public/chat/$ROOM/not-a-number")).andExpect(status().isBadRequest)
        call(get("/api-public/chat/$ROOM/$theirs"), user = 3).andExpect(status().isForbidden)
    }

    // ---------- 버그 2: 방 나가기 ----------

    @Test
    fun leave_removesMyMembershipRow_andTellsMemberService() {
        call(delete("/api-public/chat/$GROUP/member/user-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roomId").value(GROUP))
            .andExpect(jsonPath("$.members").isEmpty)

        assertThat(tables.memberIds(GROUP)).containsExactlyInAnyOrder(2L, 3L)
        val exit = argumentCaptor<com.example.chatservice.api.member.ChatRoomMemberDto>()
        verify(memberFeignClient).exitChatRoom(exit.capture())
        assertThat(exit.firstValue.chatRoomMembers.map { it.id }).containsExactly(1L)
        assertThat(exit.firstValue.chatRoomId).isNotNull()

        // 나간 방은 내 목록에서 사라지고 더는 읽을 수 없다. 남은 사람에게는 그대로다.
        call(get("/api-public/chat/1/rooms")).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].roomId").value(ROOM))
        call(get("/api-public/chat/$GROUP/room")).andExpect(status().isForbidden)
        call(get("/api-public/chat/$GROUP/room"), user = 2).andExpect(status().isOk).andExpect(jsonPath("$.members.length()").value(2))
    }

    @Test
    fun leave_lastMember_deletesTheRoom_andOthersCannotBeRemoved() {
        call(delete("/api-public/chat/$ROOM/member/user-2"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        assertThat(tables.memberIds(ROOM)).containsExactlyInAnyOrder(1L, 2L)

        call(delete("/api-public/chat/$ROOM/member/user-1")).andExpect(status().isOk)
        call(delete("/api-public/chat/$ROOM/member/user-2"), user = 2).andExpect(status().isOk)

        call(get("/api-public/chat/$ROOM/room"), user = 2).andExpect(status().isNotFound)
        assertThat(tables.count("chat")).isZero()
        assertThat(tables.count("chat_reaction")).isZero()
        call(delete("/api-public/chat/$ROOM/member/user-1")).andExpect(status().isNotFound)
    }

    // ---------- 버그 3: 메시지 삭제는 보낸 사람만 ----------

    @Test
    fun delete_onlyTheSender() {
        call(delete("/api-public/chat/$ROOM/$theirs"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_CHAT_SENDER"))
        call(delete("/api-public/chat/$ROOM/$mine"), user = 3).andExpect(status().isForbidden)
        call(delete("/api-public/chat/$ROOM/$mine"), user = null).andExpect(status().isForbidden)
        assertThat(tables.count("chat")).isEqualTo(2)

        call(delete("/api-public/chat/$ROOM/$theirs"), user = 2)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(theirs))
            .andExpect(jsonPath("$.sender").value("user-2"))

        assertThat(tables.count("chat")).isEqualTo(1)
        assertThat(tables.count("chat_reaction")).isZero()
        call(get("/api-public/chat/$ROOM/room"))
            .andExpect(jsonPath("$.lastChatId").value(mine.toString()))
            .andExpect(jsonPath("$.lastChatMsg").value("삭제된 메시지 입니다."))
    }

    // ---------- 버그 4: 오류 응답 ----------

    @Test
    fun errors_haveStatusMessageAndCode() {
        call(get("/api-public/chat/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CHAT_NOT_FOUND_ERROR"))
            .andExpect(jsonPath("$.message").isNotEmpty)
            .andExpect(jsonPath("$.status").doesNotExist())
        call(get("/api-public/chat/no-room/room")).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("CHATROOM_NOT_FOUND_ERROR"))
        call(delete("/api-public/chat/no-room/$mine")).andExpect(status().isNotFound)
        call(get("/api-public/chat/$ROOM/page/many")).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        call(post("/api-public/chat/chat/room").contentType(MediaType.APPLICATION_JSON).content("""{"not":"a list"}"""))
            .andExpect(status().isBadRequest)
        call(post("/api-public/chat/$ROOM/room").contentType(MediaType.APPLICATION_JSON).content("""{"roomName":"이름만"}"""))
            .andExpect(status().isBadRequest)
        call(post("/api-public/chat/chat/room").contentType(MediaType.APPLICATION_JSON).content("[1,2]"), user = 3)
            .andExpect(status().isForbidden)
        call(post("/api-public/chat/read/$ROOM/3"), user = 3).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("USERID_NOT_FOUND"))
        call(get("/api-public/chat/nothing/here/at/all")).andExpect(status().isNotFound)
        mockMvc.perform(get("/api-internal/chat/999999999").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isNotFound)
        mockMvc.perform(
            post("/api-internal/chat/reaction/$ROOM/$mine/user-1").header("X-Internal-Token", "test-internal-token")
                .contentType(MediaType.APPLICATION_JSON).content("""{"emoji":"LIKE"}"""),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("CANNOT_REACT_OWN_CHAT"))
        mockMvc.perform(get("/api-admin/chat/rooms/no-room").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isNotFound)
    }

    // ---------- 본인 확인 ----------

    @Test
    fun identity_pathIdMustBeTheCaller() {
        // 안드로이드는 회원 id 를, iOS 는 userId 를 넣는다. 둘 다 받는다.
        call(get("/api-public/chat/1/rooms")).andExpect(status().isOk).andExpect(jsonPath("$.length()").value(2))
        call(get("/api-public/chat/unread/1")).andExpect(status().isOk).andExpect(jsonPath("$[0].unreadChatCount").value(2))
        call(get("/api-public/chat/unread/user-1")).andExpect(status().isOk).andExpect(jsonPath("$.length()").value(2))
        call(post("/api-public/chat/read/$ROOM/user-1")).andExpect(status().isNoContent)
        call(post("/api-public/chat/read/$ROOM/1")).andExpect(status().isNoContent)
        call(get("/api-public/chat/unread/1")).andExpect(jsonPath("$[0].unreadChatCount").value(0))

        call(get("/api-public/chat/2/rooms")).andExpect(status().isForbidden)
        call(get("/api-public/chat/unread/2")).andExpect(status().isForbidden)
        call(get("/api-public/chat/unread/user-2")).andExpect(status().isForbidden)
        call(post("/api-public/chat/read/$ROOM/2")).andExpect(status().isForbidden)
        call(post("/api-public/chat/room/user-2").contentType(MediaType.TEXT_PLAIN).content("user-1")).andExpect(status().isForbidden)
    }

    @Test
    fun identity_onlyRoomMembersReadTheRoom() {
        for (path in listOf(
            "/api-public/chat/$ROOM/chats", "/api-public/chat/$ROOM/page", "/api-public/chat/$ROOM/page/30",
            "/api-public/chat/$ROOM/$theirs/30", "/api-public/chat/$ROOM/images/30", "/api-public/chat/$ROOM/count",
            "/api-public/chat/$ROOM/room", "/api-public/chat/read/$ROOM", "/api-public/chat/$theirs",
        )) {
            call(get(path)).andExpect(status().isOk)
            call(get(path), user = 3).andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("NOT_CHAT_ROOM_MEMBER"))
        }
        call(get("/api-public/chat/$ROOM/chat").param("message", "메시지"), user = 3).andExpect(status().isForbidden)
        call(get("/api-public/chat").param("ids", "$mine", "$theirs"), user = 3).andExpect(status().isOk).andExpect(content().json("[]"))
        call(post("/api-public/chat/$ROOM/member").contentType(MediaType.APPLICATION_JSON).content("""["user-3"]"""), user = 3)
            .andExpect(status().isForbidden)

        // 초대받으면 바로 읽을 수 있다.
        call(post("/api-public/chat/$ROOM/member").contentType(MediaType.APPLICATION_JSON).content("""["user-3"]"""))
            .andExpect(status().isOk)
        call(get("/api-public/chat/$ROOM/page/30"), user = 3).andExpect(status().isOk).andExpect(jsonPath("$.length()").value(2))
        verify(memberFeignClient).inviteChatRoom(any())
    }
}
