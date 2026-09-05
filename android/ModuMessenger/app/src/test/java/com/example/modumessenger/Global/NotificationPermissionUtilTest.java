package com.example.modumessenger.Global;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NotificationPermissionUtilTest {

    private static final int TIRAMISU = 33; // Build.VERSION_CODES.TIRAMISU

    @Test
    public void requestsOnApi33WhenNotGranted() {
        assertTrue(NotificationPermissionUtil.shouldRequest(TIRAMISU, false));
    }

    @Test
    public void requestsOnNewerApiWhenNotGranted() {
        assertTrue(NotificationPermissionUtil.shouldRequest(TIRAMISU + 1, false));
    }

    @Test
    public void doesNotRequestWhenAlreadyGranted() {
        assertFalse(NotificationPermissionUtil.shouldRequest(TIRAMISU, true));
    }

    @Test
    public void doesNotRequestBelowApi33EvenWhenNotGranted() {
        assertFalse(NotificationPermissionUtil.shouldRequest(TIRAMISU - 1, false));
    }

    @Test
    public void doesNotRequestBelowApi33WhenGranted() {
        assertFalse(NotificationPermissionUtil.shouldRequest(TIRAMISU - 1, true));
    }
}
