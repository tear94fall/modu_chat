package com.example.chatservice.api.support

import com.example.chatservice.api.dto.MemberDto
import com.example.chatservice.api.member.ChatRoomMemberDto
import com.example.chatservice.api.member.MemberFeignClient
import com.example.chatservice.application.member.Role
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.JdbcTemplate

/** 회원 n 의 userId 는 "user-n" 이다. */
fun memberDto(id: Long) = MemberDto(
    id = id, userId = "user-$id", auth = "google", role = Role.ROLE_MEMBER, email = "u$id@example.com",
    username = "이름$id", statusMessage = "상태$id", profileImage = "p$id.jpg", wallpaperImage = null,
)

/** member-service 흉내: user-1 … user-9 가 있고, 초대·나가기는 받은 회원을 그대로 돌려주며, 아무도 차단하지 않았다. */
fun MemberFeignClient.stubMembers() {
    val all = (1L..9L).associateWith { memberDto(it) }
    whenever(getMember(any())).thenAnswer { inv ->
        all.values.firstOrNull { it.userId == inv.getArgument<String>(0) } ?: throw NoSuchElementException("no member")
    }
    whenever(getMembersByUserId(any())).thenAnswer { inv ->
        inv.getArgument<List<String>>(0).mapNotNull { u -> all.values.firstOrNull { it.userId == u } }
    }
    whenever(getMembersById(any())).thenAnswer { inv -> inv.getArgument<List<Long>>(0).distinct().mapNotNull { all[it] } }
    whenever(inviteChatRoom(any())).thenAnswer { inv -> inv.getArgument<ChatRoomMemberDto>(0).chatRoomMembers }
    whenever(exitChatRoom(any())).thenAnswer { inv -> inv.getArgument<ChatRoomMemberDto>(0).chatRoomMembers }
    whenever(getBlockedIds(any())).thenReturn(listOf())
}

/** SQL 로 넣는 테스트 데이터. 저장소를 거치지 않아 테이블·컬럼 이름이 바뀌면 여기서 걸린다. */
class ChatTables(private val jdbc: JdbcTemplate) {

    fun clear() {
        jdbc.update("delete from chat_reaction")
        jdbc.update("delete from chat")
        jdbc.update("delete from chat_room_member")
        jdbc.update("delete from chat_room")
    }

    /** id 가 1 부터 다시 시작하게 한다(H2). 응답을 글자 그대로 비교하는 테스트용. */
    fun restartIds() {
        jdbc.update("alter table chat_room alter column chat_room_id restart with 1")
        jdbc.update("alter table chat alter column chat_id restart with 1")
        jdbc.update("alter table chat_room_member alter column chat_room_member_id restart with 1")
        jdbc.update("alter table chat_reaction alter column chat_reaction_id restart with 1")
    }

    fun room(roomId: String, name: String, lastChatId: String, vararg members: Long, lastReadChatId: String = "1") {
        jdbc.update(
            "insert into chat_room(room_id, room_name, room_image, last_chat_msg, last_chat_id, last_chat_time, created_date) " +
                "values (?,?,?,?,?,?,?)",
            roomId, name, "img-$roomId.jpg", "last of $roomId", lastChatId,
            "2026-09-20 10:00:0${members.size}", "2026-09-0${members.size} 01:02:03",
        )
        members.forEach {
            jdbc.update(
                "insert into chat_room_member(member_id, last_read_chat_id, chat_room_id) values (?,?,?)",
                it, lastReadChatId, roomPk(roomId),
            )
        }
    }

    fun chat(roomId: String, sender: String, message: String, type: Int, time: String): Long {
        jdbc.update(
            "insert into chat(chat_type, room_id, sender, message, chat_time, chat_room_id, created_date) values (?,?,?,?,?,?,?)",
            type, roomId, sender, message, time, roomPk(roomId), time,
        )
        return jdbc.queryForObject("select max(chat_id) from chat", Long::class.java)!!
    }

    fun reaction(chatId: Long, roomId: String, userId: String, emoji: String, time: String) {
        jdbc.update(
            "insert into chat_reaction(chat_id, room_id, user_id, emoji, created_date) values (?,?,?,?,?)",
            chatId, roomId, userId, emoji, time,
        )
    }

    fun setLastChat(roomId: String, lastChatId: Long) {
        jdbc.update("update chat_room set last_chat_id = ? where room_id = ?", lastChatId.toString(), roomId)
    }

    fun memberIds(roomId: String): List<Long> =
        jdbc.queryForList("select member_id from chat_room_member where chat_room_id = ?", Long::class.java, roomPk(roomId))

    fun count(table: String): Long = jdbc.queryForObject("select count(*) from $table", Long::class.java)!!

    private fun roomPk(roomId: String): Long =
        jdbc.queryForObject("select chat_room_id from chat_room where room_id = ?", Long::class.java, roomId)!!
}
