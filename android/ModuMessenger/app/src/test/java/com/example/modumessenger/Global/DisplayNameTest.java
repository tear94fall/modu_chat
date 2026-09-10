package com.example.modumessenger.Global;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DisplayNameTest {

    private FriendNames names() {
        FriendNames n = new FriendNames(new FriendNamesTest.MemoryStorage());
        n.put("u1", "지우야");
        n.put("u2", "");
        return n;
    }

    @Test
    public void 별칭이_있으면_별칭을_쓴다() {
        assertEquals("지우야", DisplayName.of(names(), "u1", "김지우"));
    }

    @Test
    public void 별칭이_비었거나_친구가_아니면_상대_이름을_쓴다() {
        assertEquals("김지우", DisplayName.of(names(), "u2", "김지우"));
        assertEquals("박민준", DisplayName.of(names(), "u3", "박민준"));
    }

    @Test
    public void 둘_다_없으면_빈_문자열이다() {
        assertEquals("", DisplayName.of(names(), "u3", null));
        assertEquals("", DisplayName.of(names(), null, null));
    }
}
