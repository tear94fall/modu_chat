package com.example.chatservice.api.golden

import com.example.chatservice.api.dto.MemberDto
import com.example.chatservice.api.member.ChatRoomMemberDto
import com.example.chatservice.api.member.MemberFeignClient
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.Role
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Qualifier
import java.io.File
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * 앱·ws-service·백오피스가 받는 응답을 글자 그대로 지킨다. 모든 API 를 한 번씩 부르고 결과를
 * `src/test/resources/golden/api-responses.txt` 와 비교한다. 그 파일은 4계층으로 나누기 전의 코드가 낸 응답에서
 * 일부러 바꾼 것(오류 상태, 고친 버그 넷, 본인 확인)만 고친 것이다.
 * 페이지 응답(`{"content":…`)은 Jackson 이 PageImpl 의 키 순서를 실행마다 다르게 낼 수 있어 JSON 으로 비교한다.
 * 응답이 일부러 바뀌었다면 build/golden-actual.txt 를 검토한 뒤 기대 파일을 바꾼다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiResponseCompatibilityTest {

    @Autowired lateinit var mockMvc: MockMvc
    lateinit var jdbc: JdbcTemplate
    @MockitoBean lateinit var memberFeignClient: MemberFeignClient
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

    @Autowired
    fun dataSource(@Qualifier("rwHikariDataSource") dataSource: DataSource) {
        jdbc = JdbcTemplate(dataSource)
    }

    private val out = StringBuilder()
    private var clearAfter = false

    @org.junit.jupiter.api.AfterEach
    fun cleanUp() {
        if (clearAfter) listOf("chat_reaction", "chat", "chat_room_member", "chat_room").forEach { jdbc.update("delete from $it") }
    }
    private val uuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private val now = Regex("20\\d\\d-\\d\\d-\\d\\d[ T]\\d\\d:\\d\\d:\\d\\d(\\.\\d+)?")

    private fun member(id: Long) = MemberDto(
        id = id, userId = "user-$id", auth = "google", role = Role.ROLE_MEMBER, email = "u$id@example.com",
        username = "이름$id", statusMessage = "상태$id", profileImage = "p$id.jpg", wallpaperImage = null,
    )

    private fun call(label: String, caller: String?, builder: MockHttpServletRequestBuilder, normalizeTime: Boolean = false) {
        if (caller != null) builder.header("X-Auth-User-Id", caller)
        if (label.contains("api-internal") || label.contains("api-admin")) builder.header("X-Internal-Token", "test-internal-token")
        out.append("### ").append(label).append('\n')
        try {
            val res = mockMvc.perform(builder).andReturn().response
            res.characterEncoding = "UTF-8"
            var body = res.contentAsString.replace(uuid, "<uuid>")
            if (normalizeTime) body = body.replace(now, "<now>")
            out.append(res.status).append(' ').append(res.contentType ?: "-").append('\n').append(body).append('\n')
        } catch (e: Exception) {
            val root = generateSequence<Throwable>(e) { it.cause }.last()
            out.append("EXCEPTION ").append(root.javaClass.simpleName).append('\n')
        }
    }

    private fun json(b: MockHttpServletRequestBuilder, body: String) = b.contentType(MediaType.APPLICATION_JSON).content(body)

    @Test
    fun golden() {
        val all = (1L..4L).associateWith { member(it) }
        whenever(memberFeignClient.getMember(any())).thenAnswer { inv ->
            all.values.first { it.userId == inv.getArgument<String>(0) }
        }
        whenever(memberFeignClient.getMembersByUserId(any())).thenAnswer { inv ->
            inv.getArgument<List<String>>(0).mapNotNull { u -> all.values.firstOrNull { it.userId == u } }
        }
        whenever(memberFeignClient.getMembersById(any())).thenAnswer { inv ->
            inv.getArgument<List<Long>>(0).distinct().mapNotNull { all[it] }
        }
        whenever(memberFeignClient.inviteChatRoom(any())).thenAnswer { inv -> inv.getArgument<ChatRoomMemberDto>(0).chatRoomMembers }
        whenever(memberFeignClient.exitChatRoom(any())).thenAnswer { inv -> inv.getArgument<ChatRoomMemberDto>(0).chatRoomMembers }
        whenever(memberFeignClient.getBlockedIds(any())).thenReturn(listOf())

        jdbc.update("delete from chat_reaction"); jdbc.update("delete from chat"); jdbc.update("delete from chat_room_member"); jdbc.update("delete from chat_room")
        jdbc.update("alter table chat_room alter column chat_room_id restart with 1")
        jdbc.update("alter table chat alter column chat_id restart with 1")
        jdbc.update("alter table chat_room_member alter column chat_room_member_id restart with 1")
        jdbc.update("alter table chat_reaction alter column chat_reaction_id restart with 1")
        fun room(roomId: String, name: String, lastId: String, vararg members: Long) {
            jdbc.update(
                "insert into chat_room(room_id, room_name, room_image, last_chat_msg, last_chat_id, last_chat_time, created_date) values (?,?,?,?,?,?,?)",
                roomId, name, "img-$roomId.jpg", "last of $roomId", lastId, "2026-09-20 10:00:0${members.size}", "2026-09-0${members.size} 01:02:03",
            )
            val pk = jdbc.queryForObject("select chat_room_id from chat_room where room_id = ?", Long::class.java, roomId)
            members.forEach { jdbc.update("insert into chat_room_member(member_id, last_read_chat_id, chat_room_id) values (?,?,?)", it, "1", pk) }
        }
        fun chat(roomId: String, sender: String, msg: String, type: Int, time: String) {
            val pk = jdbc.queryForObject("select chat_room_id from chat_room where room_id = ?", Long::class.java, roomId)
            jdbc.update(
                "insert into chat(chat_type, room_id, sender, message, chat_time, chat_room_id, created_date) values (?,?,?,?,?,?,?)",
                type, roomId, sender, msg, time, pk, time,
            )
        }
        room("room-a", "방 A", "4", 1, 2)
        room("room-b", "방 B", "6", 1, 2, 3)
        room("room-c", "방 C", "", 3)
        chat("room-a", "user-1", "hello one", 1, "2026-09-20 09:00:01")
        chat("room-a", "user-2", "hello two", 1, "2026-09-20 09:00:02")
        chat("room-a", "user-1", "photo.jpg", 2, "2026-09-20 09:00:03")
        chat("room-a", "user-2", "bye", 1, "2026-09-20 09:00:04")
        chat("room-b", "user-3", "group hello", 1, "2026-09-20 09:10:01")
        chat("room-b", "user-1", "group photo", 2, "2026-09-20 09:10:02")
        jdbc.update("insert into chat_reaction(chat_id, room_id, user_id, emoji, created_date) values (2,'room-a','user-1','LIKE','2026-09-20 09:01:00')")
        jdbc.update("insert into chat_reaction(chat_id, room_id, user_id, emoji, created_date) values (5,'room-b','user-1','HEART','2026-09-20 09:11:00')")
        jdbc.update("insert into chat_reaction(chat_id, room_id, user_id, emoji, created_date) values (5,'room-b','user-2','HEART','2026-09-20 09:11:01')")

        val me = "user-1"
        clearAfter = true
        call("GET /api-public/chat/2", me, get("/api-public/chat/2"))
        call("GET /api-public/chat/999", me, get("/api-public/chat/999"))
        call("GET /api-public/chat?ids=1,2,5", me, get("/api-public/chat").param("ids", "1", "2", "5"))
        call("GET /api-public/chat/room-a/chats", me, get("/api-public/chat/room-a/chats"))
        call("GET /api-public/chat/room-a/page?page=0&size=2", me, get("/api-public/chat/room-a/page").param("page", "0").param("size", "2"))
        call("GET /api-public/chat/room-a/page/3", me, get("/api-public/chat/room-a/page/3"))
        call("GET /api-public/chat/room-a/4/2", me, get("/api-public/chat/room-a/4/2"))
        call("GET /api-public/chat/room-a/images/5", me, get("/api-public/chat/room-a/images/5"))
        call("GET /api-public/chat/room-a/count", me, get("/api-public/chat/room-a/count"))
        call("GET /api-public/chat/no-room/count", me, get("/api-public/chat/no-room/count"))
        call("GET /api-public/chat/no-room/page/3", me, get("/api-public/chat/no-room/page/3"))
        call("GET /api-public/chat/room-a/2", me, get("/api-public/chat/room-a/2"))
        call("GET /api-public/chat/room-a/chat?message=hello", me, get("/api-public/chat/room-a/chat").param("message", "hello"))
        call("GET /api-public/chat/1/rooms", me, get("/api-public/chat/1/rooms"))
        call("GET /api-public/chat/room-a/room", me, get("/api-public/chat/room-a/room"))
        call("GET /api-public/chat/no-room/room", me, get("/api-public/chat/no-room/room"))
        call("GET /api-public/chat/unread/1", me, get("/api-public/chat/unread/1"))
        call("GET /api-public/chat/read/room-a", me, get("/api-public/chat/read/room-a"))
        call("POST /api-public/chat/read/room-a/1", me, post("/api-public/chat/read/room-a/1"))
        call("POST /api-public/chat/read/room-b/user-1", me, post("/api-public/chat/read/room-b/user-1"))
        call("GET /api-public/chat/unread/1 (after read)", me, get("/api-public/chat/unread/1"))
        call("GET /api-public/chat/read/room-b", me, get("/api-public/chat/read/room-b"))
        call("POST /api-public/chat/room/user-1", me, post("/api-public/chat/room/user-1").contentType(MediaType.TEXT_PLAIN).content("user-2"))
        call(
            "POST /api-public/chat/room-a/room", me,
            json(post("/api-public/chat/room-a/room"), """{"roomName":"새 이름","roomImage":"new.jpg","lastChatMsg":"bye","lastChatId":"4","lastChatTime":"2026-09-20 09:00:04"}"""),
        )
        call("POST /api-public/chat/chat/room [1,2]", me, json(post("/api-public/chat/chat/room"), "[1,2]"))
        call("POST /api-public/chat/chat/room [1,4,4]", me, json(post("/api-public/chat/chat/room"), "[1,4,4]"), normalizeTime = true)
        jdbc.update("update chat_room set last_chat_time = '2026-09-21 00:00:00', created_date = '2026-09-21 00:00:00' where chat_room_id = 4")
        call("POST /api-public/chat/chat/room [99]", me, json(post("/api-public/chat/chat/room"), "[99]"))
        call("POST /api-public/chat/room-a/member", me, json(post("/api-public/chat/room-a/member"), """["user-2","user-3"]"""))
        call("GET /api-public/chat/room-a/room (after invite)", me, get("/api-public/chat/room-a/room"))
        call("DELETE /api-public/chat/room-a/3", me, delete("/api-public/chat/room-a/3"))
        call("GET /api-public/chat/room-a/room (after delete chat)", me, get("/api-public/chat/room-a/room"))
        call("DELETE /api-public/chat/room-b/member/user-1", me, delete("/api-public/chat/room-b/member/user-1"))
        call("GET /api-public/chat/1/rooms (after leave)", me, get("/api-public/chat/1/rooms"))

        call(
            "POST /api-internal/chat", null,
            json(post("/api-internal/chat"), """{"chatType":1,"roomId":"room-a","sender":"user-2","message":"saved","chatTime":"2026-09-20 11:00:00"}"""),
        )
        call("POST /api-internal/chat (no room)", null, json(post("/api-internal/chat"), """{"chatType":1,"roomId":"nope","sender":"user-2","message":"x","chatTime":"t"}"""))
        call("GET /api-internal/chat/7", null, get("/api-internal/chat/7"))
        call("GET /api-internal/chat/room-a/room", null, get("/api-internal/chat/room-a/room"))
        call("GET /api-internal/chat/room-a/member/user-1", null, get("/api-internal/chat/room-a/member/user-1"))
        call("GET /api-internal/chat/room-a/member/user-4", null, get("/api-internal/chat/room-a/member/user-4"))
        call(
            "POST /api-internal/chat/room-a/room", null,
            json(post("/api-internal/chat/room-a/room"), """{"roomName":"방 A","roomImage":"","lastChatMsg":"saved","lastChatId":"7","lastChatTime":"2026-09-20 11:00:00"}"""),
        )
        call("POST /api-internal/chat/reaction/room-a/2/user-1 LIKE(cancel)", null, json(post("/api-internal/chat/reaction/room-a/2/user-1"), """{"emoji":"like"}"""))
        call("POST /api-internal/chat/reaction/room-a/7/user-1 WOW", null, json(post("/api-internal/chat/reaction/room-a/7/user-1"), """{"emoji":"WOW"}"""))
        call("POST /api-internal/chat/reaction/room-a/7/user-1 SAD(replace)", null, json(post("/api-internal/chat/reaction/room-a/7/user-1"), """{"emoji":"SAD"}"""))
        call("POST /api-internal/chat/reaction/room-a/7/user-2 (own)", null, json(post("/api-internal/chat/reaction/room-a/7/user-2"), """{"emoji":"SAD"}"""))
        call("GET /api-internal/chat/7 (after reaction)", null, get("/api-internal/chat/7"))
        call("POST /api-internal/chat/read/room-a/user-2", null, post("/api-internal/chat/read/room-a/user-2"))
        call("POST /api-internal/chat/read/room-a/user-4 (not member)", null, post("/api-internal/chat/read/room-a/user-4"))
        call("GET /api-public/chat/read/room-a (after internal read)", me, get("/api-public/chat/read/room-a"))

        call("GET /api-admin/chat/rooms", null, get("/api-admin/chat/rooms"), normalizeTime = false)
        call("GET /api-admin/chat/rooms?sort=memberCount,desc&size=2", null, get("/api-admin/chat/rooms").param("sort", "memberCount,desc").param("size", "2"))
        call("GET /api-admin/chat/rooms?sort=roomName,asc&page=1&size=2", null, get("/api-admin/chat/rooms").param("sort", "roomName,asc").param("page", "1").param("size", "2"))
        call("GET /api-admin/chat/rooms?sort=bad", null, get("/api-admin/chat/rooms").param("sort", "bad"))
        call("GET /api-admin/chat/rooms/room-a", null, get("/api-admin/chat/rooms/room-a"))
        call("GET /api-admin/chat/rooms/no-room", null, get("/api-admin/chat/rooms/no-room"))
        call("GET /api-admin/chat/rooms/room-a/chats?size=3", null, get("/api-admin/chat/rooms/room-a/chats").param("size", "3"))

        call("DELETE /api-internal/chat/member/3/rooms", null, delete("/api-internal/chat/member/3/rooms"))
        call("GET /api-admin/chat/rooms (after withdraw)", null, get("/api-admin/chat/rooms"))

        File("build/golden-actual.txt").writeText(out.toString())
        val expected = javaClass.getResource("/golden/api-responses.txt")!!.readText().trimEnd().lines()
        val actual = out.toString().trimEnd().lines()
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        org.assertj.core.api.Assertions.assertThat(actual).hasSameSizeAs(expected)
        var label = ""
        expected.zip(actual).forEach { (e, a) ->
            if (e.startsWith("### ")) label = e
            if (e.startsWith("{\"content\":")) {
                org.assertj.core.api.Assertions.assertThat(mapper.readTree(a)).describedAs(label).isEqualTo(mapper.readTree(e))
            } else {
                org.assertj.core.api.Assertions.assertThat(a).describedAs(label).isEqualTo(e)
            }
        }
    }
}
