package com.example.modumessenger.Global;

import android.os.Build;

public class NotificationPermissionUtil {

    /**
     * POST_NOTIFICATIONS 런타임 권한을 요청해야 하는지 판단한다.
     * API 33(TIRAMISU) 미만에는 이 권한 자체가 없으므로 절대 요청하지 않는다.
     * param : sdkInt 현재 기기의 Build.VERSION.SDK_INT
     * param : alreadyGranted 현재 권한이 이미 허용되어 있는지 여부
     * return : 요청을 시도해야 하면 true
     */
    public static boolean shouldRequest(int sdkInt, boolean alreadyGranted) {
        return sdkInt >= Build.VERSION_CODES.TIRAMISU && !alreadyGranted;
    }
}
