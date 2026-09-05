package com.example.modumessenger.Global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.modumessenger.entity.Member;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatRoomNameUtilTest {

    private static final String ME = "me";
    private static final String MY_NAME = "임준섭";

    private Member member(String userId, String username) {
        return new Member(userId, userId + "@test.com", username, "", "", "");
    }

    @Test
    public void 직접_지은_이름이_있으면_참여자가_많아도_그_이름을_쓴다() {
        List<Member> members = Arrays.asList(
                member(ME, MY_NAME), member("a", "친구1"), member("b", "친구2"));

        assertEquals("우리 팀 방",
                ChatRoomNameUtil.resolve("우리 팀 방", members, ME, MY_NAME, 0));
    }

    @Test
    public void 이름이_비어있으면_참여자_이름으로_만든다() {
        List<Member> members = Arrays.asList(member(ME, MY_NAME), member("a", "친구1"));

        assertEquals("친구1", ChatRoomNameUtil.resolve("", members, ME, MY_NAME, 0));
    }

    @Test
    public void 기본_이름은_직접_지은_이름으로_보지_않는다() {
        List<Member> members = Arrays.asList(
                member(ME, MY_NAME), member("a", "친구1"), member("b", "친구2"));

        assertEquals("친구1, 친구2",
                ChatRoomNameUtil.resolve(ChatRoomNameUtil.DEFAULT_ROOM_NAME, members, ME, MY_NAME, 0));
    }

    @Test
    public void 나_혼자면_나와의_채팅으로_보인다() {
        List<Member> members = Collections.singletonList(member(ME, MY_NAME));

        assertEquals("나와의 채팅 (임준섭)",
                ChatRoomNameUtil.resolve("", members, ME, MY_NAME, 0));
    }

    @Test
    public void 목록처럼_길이가_제한된_곳에서는_자르고_끝의_쉼표를_없앤다() {
        List<Member> members = Arrays.asList(
                member(ME, MY_NAME), member("a", "친구1"), member("b", "친구2"), member("c", "친구3"));

        // "친구1, 친구2, 친구3" 을 10 자로 자르면 "친구1, 친구2," 라 끝의 쉼표를 떼야 한다.
        assertEquals("친구1, 친구2",
                ChatRoomNameUtil.resolve("", members, ME, MY_NAME, 10));
    }

    @Test
    public void 직접_지은_이름_판별() {
        assertFalse(ChatRoomNameUtil.hasCustomName(null));
        assertFalse(ChatRoomNameUtil.hasCustomName(""));
        assertFalse(ChatRoomNameUtil.hasCustomName("   "));
        assertFalse(ChatRoomNameUtil.hasCustomName(ChatRoomNameUtil.DEFAULT_ROOM_NAME));
        assertTrue(ChatRoomNameUtil.hasCustomName("우리 팀 방"));
    }

    @Test
    public void 직접_넣은_방_사진_판별() {
        assertFalse(ChatRoomNameUtil.hasCustomImage(null));
        assertFalse(ChatRoomNameUtil.hasCustomImage(""));
        assertFalse(ChatRoomNameUtil.hasCustomImage("   "));
        assertTrue(ChatRoomNameUtil.hasCustomImage("df5c7f1f.png"));
    }

    @Test
    public void 참여자가_없어도_터지지_않는다() {
        assertEquals("나와의 채팅 (임준섭)",
                ChatRoomNameUtil.resolve("", null, ME, MY_NAME, 0));
    }
}
