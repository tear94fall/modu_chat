package com.example.wsservice.fcm.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wsservice.chat.dto.ChatDto;
import com.example.wsservice.chat.dto.ChatRoomDto;
import com.example.wsservice.member.dto.MemberDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class FcmMessageDtoTest {

    private MemberDto member(String userId, String username) {
        MemberDto m = new MemberDto();
        m.setUserId(userId);
        m.setUsername(username);
        return m;
    }

    @Test
    void 데이터에_발신자_이름과_참여자_수를_넣는다() {
        ChatRoomDto room = new ChatRoomDto();
        room.setRoomId("room-1");
        room.setRoomName("우리 방");
        room.setMembers(List.of(member("u1", "김지우"), member("u2", "임준섭"), member("u3", "박민준")));
        ChatDto chat = new ChatDto();
        chat.setSender("u1");
        chat.setMessage("안녕");

        FcmMessageDto dto = new FcmMessageDto(room, chat);

        assertThat(dto.getTitle()).isEqualTo("우리 방");
        assertThat(dto.getData()).containsEntry("roomId", "room-1")
                .containsEntry("sender", "u1")
                .containsEntry("senderName", "김지우")
                .containsEntry("memberCount", "3");
    }

    @Test
    void 발신자가_참여자에_없으면_이름은_빈_문자열이다() {
        ChatRoomDto room = new ChatRoomDto();
        room.setRoomId("room-1");
        room.setMembers(List.of(member("u2", "임준섭")));
        ChatDto chat = new ChatDto();
        chat.setSender("ghost");

        assertThat(new FcmMessageDto(room, chat).getData()).containsEntry("senderName", "").containsEntry("memberCount", "1");
    }

    @Test
    void 직접_만드는_생성자는_제목을_잃지_않는다() {
        FcmMessageDto dto = new FcmMessageDto("room-1", 0, "제목", "본문", null, "u1");

        assertThat(dto.getTopic()).isEqualTo("room-1");
        assertThat(dto.getTitle()).isEqualTo("제목");
    }
}
