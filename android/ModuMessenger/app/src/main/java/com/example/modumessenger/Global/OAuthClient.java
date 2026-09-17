package com.example.modumessenger.Global;

import java.util.HashMap;
import java.util.Map;

/** auth-service OAuth2 토큰 엔드포인트에 보내는 폼 파라미터. 채팅 앱의 client_id 는 modu-chat. */
public final class OAuthClient {

    public static final String CLIENT_ID = "modu-chat";
    public static final String GRANT_GOOGLE = "urn:modu:params:oauth:grant-type:google_id_token";
    public static final String GRANT_REFRESH = "refresh_token";

    private OAuthClient() {}

    public static Map<String, String> googleForm(String idToken) {
        Map<String, String> form = new HashMap<>();
        form.put("grant_type", GRANT_GOOGLE);
        form.put("client_id", CLIENT_ID);
        form.put("id_token", idToken);
        return form;
    }

    public static Map<String, String> refreshForm(String refreshToken) {
        Map<String, String> form = new HashMap<>();
        form.put("grant_type", GRANT_REFRESH);
        form.put("client_id", CLIENT_ID);
        form.put("refresh_token", stripBearer(refreshToken));
        return form;
    }

    /** DataStore 에는 "Bearer xxx" 로 저장돼 있다. 폼 파라미터에는 값만 보낸다. */
    public static String stripBearer(String stored) {
        if (stored == null) return "";
        return stored.startsWith("Bearer ") ? stored.substring("Bearer ".length()) : stored;
    }
}
