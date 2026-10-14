package com.example.memberservice.application.usecase

import com.example.memberservice.application.port.PushPort
import com.example.memberservice.application.service.NoticeCommandService
import com.example.memberservice.application.service.NoticeQueryService
import com.example.memberservice.application.usecase.command.CreateNoticeCommand
import com.example.memberservice.application.usecase.result.NoticeResult
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/** 공지사항. 앱 목록·상세는 레플리카, 백오피스 목록은 등록 직후에 다시 읽으므로 master. */
@Component
class NoticeUseCase(
    private val noticeQueryService: NoticeQueryService,
    private val noticeCommandService: NoticeCommandService,
    private val pushPort: PushPort,
) {

    private val log = LoggerFactory.getLogger(NoticeUseCase::class.java)

    fun getNotices(): List<NoticeResult> = noticeQueryService.getNotices()

    fun getNotice(id: Long): NoticeResult? = noticeQueryService.getNotice(id)

    fun searchNotices(pageable: Pageable): Page<NoticeResult> = noticeCommandService.searchNotices(pageable)

    /**
     * 공지를 저장하고(커밋), 요청이면 전체 푸시로도 내보낸다 — 푸시는 트랜잭션 밖이다.
     * 푸시가 실패해도 공지는 남긴다 — 글이 사라지는 것보다 알림이 못 간 편이 낫고,
     * 알림을 놓쳐도 사용자가 공지사항에서 다시 볼 수 있다.
     */
    fun createNotice(command: CreateNoticeCommand): NoticeResult {
        val saved = noticeCommandService.createNotice(command)

        if (command.push) {
            try {
                pushPort.broadcast(
                    command.title, command.content,
                    mapOf("type" to "notice", "noticeId" to saved.id.toString()),
                )
            } catch (e: Exception) {
                log.warn("공지 {} 저장은 됐지만 푸시 발송에 실패했다: {}", saved.id, e.message)
            }
        }

        return saved
    }
}
