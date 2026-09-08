package com.example.modumessenger.Global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatSenderUtilTest {

    private Member member(String userId, String username) {
        return new Member(userId, userId + "@test.com", username, "", "img-" + userId, "");
    }

    @Test
    public void 참여자_목록에_있는_보낸_사람은_그대로_돌려준다() {
        Member friend = member("friend", "친구");
        List<Member> members = Arrays.asList(member("me", "나"), friend);

        assertSame(friend, ChatSenderUtil.resolve(members, "friend"));
    }

    @Test
    public void 참여자_목록이_아직_비어_있으면_자리표시자를_돌려주고_null이_아니다() {
        Member placeholder = ChatSenderUtil.resolve(Collections.emptyList(), "friend");

        assertNotNull(placeholder);
        assertEquals("friend", placeholder.getUserId());
        assertNull(placeholder.getId());
        assertNotNull(placeholder.getUsername());
        assertNotNull(placeholder.getProfileImage());
        assertFalse(ChatSenderUtil.isKnown(placeholder));
    }

    @Test
    public void 방을_나간_사람의_메시지도_자리표시자로_처리한다() {
        List<Member> members = Collections.singletonList(member("me", "나"));

        Member placeholder = ChatSenderUtil.resolve(members, "left-user");

        assertEquals("left-user", placeholder.getUserId());
        assertEquals(ChatSenderUtil.UNKNOWN_NAME, placeholder.getUsername());
        assertFalse(ChatSenderUtil.isKnown(placeholder));
    }

    @Test
    public void 서버에서_받은_회원은_id가_있어_아는_사람으로_본다() {
        MemberDto dto = new MemberDto();
        dto.setId(50L);
        dto.setUserId("friend");
        dto.setUsername("친구");

        assertTrue(ChatSenderUtil.isKnown(new Member(dto)));
    }

    @Test
    public void 목록이_null이어도_터지지_않는다() {
        assertNotNull(ChatSenderUtil.resolve(null, "friend"));
    }
}
