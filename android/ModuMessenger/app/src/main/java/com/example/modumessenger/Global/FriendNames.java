package com.example.modumessenger.Global;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

/**
 * 내가 친구에게 붙인 이름(userId → 별칭). 서버 GET /friends/names 로 받아 메모리와 DataStore 에 둔다.
 * FCM 이 콜드 스타트한 프로세스에는 메모리 맵이 없으므로 저장소에서 복원한다.
 */
public final class FriendNames {

    public interface Storage {
        String read();
        void write(String json);
    }

    private static final String STORE_KEY = "friend-names";
    private static volatile FriendNames instance;

    private final Storage storage;
    private final Map<String, String> names = new HashMap<>();
    /** put/replaceAll 마다 1씩 오른다. 화면은 마지막으로 그린 버전과 비교해 다시 그릴지 정한다. */
    private int version;

    public FriendNames(Storage storage) {
        this.storage = storage;
        restore();
    }

    /** 앱 전역 인스턴스. DataStore 에 JSON 한 덩어리로 저장한다. */
    public static FriendNames instance() {
        if (instance == null) {
            synchronized (FriendNames.class) {
                if (instance == null) {
                    instance = new FriendNames(new Storage() {
                        @Override public String read() {
                            try { return DataStoreHelper.getDataStoreStr(STORE_KEY); } catch (Exception e) { return null; }
                        }
                        @Override public void write(String json) {
                            try { DataStoreHelper.setDataStoreObject(STORE_KEY, json); } catch (Exception ignored) { }
                        }
                    });
                }
            }
        }
        return instance;
    }

    public synchronized void replaceAll(Map<String, String> fresh) {
        names.clear();
        if (fresh != null) names.putAll(fresh);
        version++;
        persist();
    }

    public synchronized void put(String userId, String name) {
        if (userId == null) return;
        names.put(userId, name == null ? "" : name);
        version++;
        persist();
    }

    /** 별칭. 친구가 아니면 null, 별칭을 지운 친구면 "". */
    public synchronized String get(String userId) {
        return userId == null ? null : names.get(userId);
    }

    public synchronized int version() {
        return version;
    }

    public synchronized Map<String, String> snapshot() {
        return new HashMap<>(names);
    }

    private void restore() {
        String json = storage.read();
        if (json == null || json.isEmpty()) return;
        try {
            Map<String, String> saved = new Gson().fromJson(json, new TypeToken<Map<String, String>>() {}.getType());
            if (saved != null) names.putAll(saved);
        } catch (RuntimeException ignored) {
            // 깨진 저장값은 버리고 빈 맵으로 시작한다. 다음 서버 조회가 다시 채운다.
        }
    }

    private void persist() {
        storage.write(new Gson().toJson(names));
    }
}
