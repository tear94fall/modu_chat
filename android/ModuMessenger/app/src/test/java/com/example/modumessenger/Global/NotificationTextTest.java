package com.example.modumessenger.Global;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NotificationTextTest {

    private FriendNames names() {
        FriendNames n = new FriendNames(new FriendNamesTest.MemoryStorage());
        n.put("u1", "지우야");
        return n;
    }

    @Test
    public void 일대일_방이면_제목이_발신자_별칭이고_본문은_메시지다() {
        NotificationText t = NotificationText.build(names(), "새로운 채팅방", "u1", "김지우", "2", "안녕", false);
        assertEquals("지우야", t.getTitle());
        assertEquals("안녕", t.getBody());
    }

    @Test
    public void 단톡방이면_제목은_방_이름이고_본문은_발신자와_메시지다() {
        NotificationText t = NotificationText.build(names(), "우리 팀", "u1", "김지우", "3", "안녕", false);
        assertEquals("우리 팀", t.getTitle());
        assertEquals("지우야: 안녕", t.getBody());
    }

    @Test
    public void 별칭이_없으면_서버가_준_이름을_쓰고_그것도_없으면_방_이름만_쓴다() {
        assertEquals("박민준", NotificationText.build(names(), "방", "u9", "박민준", "2", "hi", false).getTitle());
        NotificationText t = NotificationText.build(names(), "방", "u9", "", "2", "hi", false);
        assertEquals("방", t.getTitle());
        assertEquals("hi", t.getBody());
    }

    @Test
    public void 사진이면_본문은_새로운_사진이다() {
        assertEquals("지우야: 새로운 사진", NotificationText.build(names(), "팀", "u1", "김지우", "5", "x.png", true).getBody());
        assertEquals("새로운 사진", NotificationText.build(names(), "팀", "u1", "김지우", "2", "x.png", true).getBody());
    }

    @Test
    public void 참여자_수를_모르면_단톡방_형식이다() {
        assertEquals("방", NotificationText.build(names(), "방", "u1", "김지우", null, "hi", false).getTitle());
    }
}
