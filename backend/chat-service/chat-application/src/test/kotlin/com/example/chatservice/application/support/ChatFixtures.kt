package com.example.chatservice.application.support

import com.example.chatservice.application.domain.entity.Chat
import com.example.chatservice.application.domain.entity.ChatReaction
import com.example.chatservice.application.domain.entity.ChatRoom
import com.example.chatservice.application.domain.entity.ChatRoomMember
import com.example.chatservice.application.domain.entity.ChatType
import com.example.chatservice.application.domain.entity.ReactionEmoji
import com.example.chatservice.application.domain.repository.rw.ChatReactionRwRepository
import com.example.chatservice.application.domain.repository.rw.ChatRoomMemberRwRepository
import com.example.chatservice.application.domain.repository.rw.ChatRoomRwRepository
import com.example.chatservice.application.domain.repository.rw.ChatRwRepository
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * 테스트 데이터. master 에 바로 커밋한다 — replica 쪽 저장소(다른 커넥션)에서도 보여야 하기 때문이다.
 * 그래서 테스트는 @Transactional 로 되돌리지 않고 [clear] 로 지운다.
 */
@Component
class ChatFixtures(
    private val chatRoomRepository: ChatRoomRwRepository,
    private val chatRoomMemberRepository: ChatRoomMemberRwRepository,
    private val chatRepository: ChatRwRepository,
    private val chatReactionRepository: ChatReactionRwRepository,
    @Qualifier("rwHikariDataSource") private val dataSource: DataSource,
) {

    fun clear() {
        chatReactionRepository.deleteAllInBatch()
        chatRepository.deleteAllInBatch()
        chatRoomMemberRepository.deleteAllInBatch()
        chatRoomRepository.deleteAllInBatch()
    }

    fun room(
        roomId: String,
        vararg memberIds: Long,
        roomName: String = roomId,
        lastChatMsg: String = "",
        lastChatId: String = "",
        lastReadChatId: String = "",
    ): ChatRoom {
        val room = chatRoomRepository.save(ChatRoom(roomId, roomName, "", lastChatMsg, lastChatId, "2026-09-13 00:00:00"))
        for (memberId in memberIds) {
            chatRoomMemberRepository.save(ChatRoomMember(memberId, lastReadChatId, room))
        }
        return room
    }

    fun chat(room: ChatRoom, sender: String?, message: String, type: Int = ChatType.CHAT_TYPE_TEXT): Chat =
        chatRepository.save(Chat(message, room.roomId, room, sender, "2026-09-13 00:00:00", type))

    fun reaction(chat: Chat, userId: String, emoji: ReactionEmoji): ChatReaction =
        chatReactionRepository.save(ChatReaction(chat.id, chat.roomId, userId, emoji))

    /** 방의 마지막 메시지 id 와 멤버들의 읽음 커서를 맞춘다. */
    fun lastChat(roomId: String, lastChatId: Long, lastReadChatId: Long) {
        val room = chatRoomRepository.findByRoomId(roomId).orElseThrow()
        room.updateChatRoom(room.roomName, room.roomImage, room.lastChatMsg, lastChatId.toString(), room.lastChatTime)
        chatRoomRepository.save(room)
        chatRoomMemberRepository.findAll().filter { it.chatRoom?.id == room.id }.forEach {
            it.updateLastReadChatId(lastReadChatId.toString())
            chatRoomMemberRepository.save(it)
        }
    }

    fun memberIdsOf(roomId: String): List<Long> =
        JdbcTemplate(dataSource).queryForList(
            "select m.member_id from chat_room_member m join chat_room r on r.chat_room_id = m.chat_room_id where r.room_id = ?",
            Long::class.java,
            roomId,
        )

    fun roomExists(roomId: String): Boolean = chatRoomRepository.findByRoomId(roomId).isPresent
}
