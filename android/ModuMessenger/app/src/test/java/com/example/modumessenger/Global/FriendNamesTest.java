package com.example.modumessenger.Global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class FriendNamesTest {

    /** 문자열 하나만 기억하는 가짜 저장소. */
    static class MemoryStorage implements FriendNames.Storage {
        String json;
        @Override public String read() { return json; }
        @Override public void write(String json) { this.json = json; }
    }

    @Test
    public void 넣은_이름을_돌려주고_없으면_null이다() {
        FriendNames names = new FriendNames(new MemoryStorage());
        names.put("u1", "지우야");

        assertEquals("지우야", names.get("u1"));
        assertNull(names.get("u2"));
    }

    @Test
    public void 전체_교체와_저장소_복원이_된다() {
        MemoryStorage storage = new MemoryStorage();
        FriendNames names = new FriendNames(storage);
        Map<String, String> map = new HashMap<>();
        map.put("u1", "지우야");
        map.put("u2", "");
        names.replaceAll(map);

        FriendNames restored = new FriendNames(storage);
        assertEquals("지우야", restored.get("u1"));
        assertEquals("", restored.get("u2"));
        assertEquals(2, restored.snapshot().size());
    }

    @Test
    public void 저장소가_비었거나_깨져_있어도_빈_맵으로_시작한다() {
        MemoryStorage broken = new MemoryStorage();
        broken.json = "not json";

        assertEquals(0, new FriendNames(broken).snapshot().size());
        assertEquals(0, new FriendNames(new MemoryStorage()).snapshot().size());
    }

    @Test
    public void 이름이_바뀔_때마다_버전이_올라가서_화면이_다시_그릴지_알_수_있다() {
        FriendNames names = new FriendNames(new MemoryStorage());
        int v0 = names.version();

        names.put("u1", "지우");
        int v1 = names.version();
        names.replaceAll(new HashMap<>());
        int v2 = names.version();

        assertTrue(v1 > v0);
        assertTrue(v2 > v1);
        assertEquals(v2, names.version()); // 읽기만으로는 바뀌지 않는다
    }
}
