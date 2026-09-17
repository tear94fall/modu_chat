package com.example.modumessenger.Global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SsoRequestTest {

    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    @Test
    public void 허용된_클라이언트와_S256_챌린지만_받는다() {
        SsoRequest ok = SsoRequest.from("modu-commerce", CHALLENGE, "S256");
        assertNotNull(ok);
        assertEquals("modu-commerce", ok.getClientId());

        assertNull(SsoRequest.from("modu-admin", CHALLENGE, "S256"));
        assertNull(SsoRequest.from("modu-commerce", "short", "S256"));
        assertNull(SsoRequest.from("modu-commerce", CHALLENGE, "plain"));
        assertNull(SsoRequest.from(null, CHALLENGE, "S256"));
    }

    @Test
    public void 호출_앱은_허용_목록에_있어야_한다() {
        assertTrue(SsoRequest.isAllowedCaller("com.example.moducommerce"));
        assertFalse(SsoRequest.isAllowedCaller("com.evil.app"));
        assertFalse(SsoRequest.isAllowedCaller(null));
    }
}
