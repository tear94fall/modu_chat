package com.example.modumessenger.Global;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Map;

public class OAuthClientTest {

    @Test
    public void 구글_토큰_교환_폼은_grant_client_id_token_을_담는다() {
        Map<String, String> form = OAuthClient.googleForm("id-token");
        assertEquals("urn:modu:params:oauth:grant-type:google_id_token", form.get("grant_type"));
        assertEquals("modu-chat", form.get("client_id"));
        assertEquals("id-token", form.get("id_token"));
        assertEquals(3, form.size());
    }

    @Test
    public void 리프레시_폼은_저장된_Bearer_접두어를_뗀다() {
        Map<String, String> form = OAuthClient.refreshForm("Bearer rt-1");
        assertEquals("refresh_token", form.get("grant_type"));
        assertEquals("modu-chat", form.get("client_id"));
        assertEquals("rt-1", form.get("refresh_token"));
    }

    @Test
    public void stripBearer_는_접두어가_없거나_null_이어도_안전하다() {
        assertEquals("abc", OAuthClient.stripBearer("abc"));
        assertEquals("", OAuthClient.stripBearer(null));
    }
}
