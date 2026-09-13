package com.example.modumessenger.push

/**
 * 알림을 띄우지 않을지 판단한다(스펙 §7 + 설계 §3). 화면도 서비스도 없는 순수 규칙이라 그대로 시험한다.
 *
 * 억제하는 경우 세 가지:
 * 1. 발신자가 나 자신일 때(다른 기기에서 내가 보낸 메시지의 에코).
 * 2. 발신자를 내가 차단했을 때 — 차단은 서버 흐름을 바꾸지 않고 앱에서 거른다.
 * 3. 앱이 앞에 떠 있고 웹소켓도 붙어 있을 때 — 그때는 인앱 배너가 대신 알린다.
 */
object PushSuppression {

    fun shouldSuppress(
        sender: String?,
        myUserId: String?,
        blockedIds: Set<String>,
        isForeground: Boolean,
        isSocketConnected: Boolean,
    ): Boolean {
        if (!sender.isNullOrBlank()) {
            if (sender == myUserId) return true
            if (sender in blockedIds) return true
        }
        return isForeground && isSocketConnected
    }
}
