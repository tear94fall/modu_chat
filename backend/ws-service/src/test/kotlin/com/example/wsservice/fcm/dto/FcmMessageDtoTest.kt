package com.example.wsservice.fcm.dto

import com.example.wsservice.chat.dto.ChatDto
import com.example.wsservice.chat.dto.ChatRoomDto
import com.example.wsservice.member.dto.MemberDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FcmMessageDtoTest {

    private fun member(userId: String, username: String) = MemberDto(userId = userId, username = username)

    @Test
    fun 데이터에_발신자_이름과_참여자_수를_넣는다() {
        val room = ChatRoomDto(roomId = "room-1", roomName = "우리 방", members = listOf(member("u1", "김지우"), member("u2", "임준섭"), member("u3", "박민준")))
        val chat = ChatDto(sender = "u1", message = "안녕")

        val dto = FcmMessageDto(room, chat)

        assertThat(dto.title).isEqualTo("우리 방")
        assertThat(dto.data).containsEntry("roomId", "room-1")
            .containsEntry("sender", "u1")
            .containsEntry("senderName", "김지우")
            .containsEntry("memberCount", "3")
    }

    @Test
    fun 발신자가_참여자에_없으면_이름은_빈_문자열이다() {
        val room = ChatRoomDto(roomId = "room-1", members = listOf(member("u2", "임준섭")))
        val chat = ChatDto(sender = "ghost")

        assertThat(FcmMessageDto(room, chat).data).containsEntry("senderName", "").containsEntry("memberCount", "1")
    }

    @Test
    fun 직접_만드는_생성자는_제목을_잃지_않는다() {
        val dto = FcmMessageDto("room-1", 0, "제목", "본문", null, "u1")

        assertThat(dto.topic).isEqualTo("room-1")
        assertThat(dto.title).isEqualTo("제목")
    }
}
