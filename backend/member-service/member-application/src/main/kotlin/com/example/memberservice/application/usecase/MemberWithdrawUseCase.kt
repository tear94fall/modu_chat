package com.example.memberservice.application.usecase

import com.example.memberservice.application.port.ChatRoomPort
import com.example.memberservice.application.port.CommercePort
import com.example.memberservice.application.port.PushPort
import com.example.memberservice.application.port.StoragePort
import com.example.memberservice.application.service.MemberCommandService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 회원 탈퇴(본인만 — 본인 확인은 member-api 의 AuthUserInterceptor 가 한다).
 *
 * 순서는 예전과 같다: 채팅방 나가기 → 푸시 토큰 삭제 → 커머스 고객 정리 → 사진 파일 삭제 → 친구 관계 삭제·탈퇴 표시(DB).
 * 다른 서비스 호출은 모두 DB 트랜잭션 밖에서 하고, DB 작업은 마지막에 한 트랜잭션으로 끝낸다.
 *
 * - 방 나가기·푸시 토큰 삭제가 실패하면 예외가 그대로 올라가 탈퇴도 되지 않는다 — 앱이 다시 시도한다.
 * - 커머스 고객 정리·사진 파일 삭제는 실패해도 탈퇴를 막지 않는다(로그만 남긴다).
 * - 이미 탈퇴한 회원이면 아무것도 하지 않는다(멱등). 중간에 실패한 탈퇴를 다시 부르면 처음부터 다시 한다 —
 *   각 서비스의 정리는 여러 번 불러도 같다.
 */
@Component
class MemberWithdrawUseCase(
    private val memberCommandService: MemberCommandService,
    private val chatRoomPort: ChatRoomPort,
    private val pushPort: PushPort,
    private val commercePort: CommercePort,
    private val storagePort: StoragePort,
) {

    private val log = LoggerFactory.getLogger(MemberWithdrawUseCase::class.java)

    fun withdraw(userId: String) {
        val target = memberCommandService.findWithdrawalTarget(userId) ?: return

        chatRoomPort.exitAllChatRooms(target.memberId)
        pushPort.deleteToken(userId)
        deleteCommerceCustomerQuietly(userId)
        deleteFileQuietly(target.profileImage)
        deleteFileQuietly(target.wallpaperImage)

        memberCommandService.completeWithdrawal(target.memberId)
    }

    /** 커머스는 다른 제품이라 그쪽 장애로 메신저 탈퇴가 막히면 안 된다. 실패·시간 초과(3초)는 로그만 남긴다. */
    private fun deleteCommerceCustomerQuietly(userId: String) {
        try {
            commercePort.deleteCustomer(userId)
        } catch (e: Exception) {
            log.warn("탈퇴 회원의 커머스 고객 정리 실패, 계속 진행 userId={} message={}", userId, e.message)
        }
    }

    private fun deleteFileQuietly(file: String?) {
        if (file.isNullOrBlank()) return
        try {
            storagePort.delete(file)
        } catch (e: RuntimeException) {
            log.warn("탈퇴 회원의 파일 삭제 실패, 계속 진행 file={} message={}", file, e.message)
        }
    }
}
