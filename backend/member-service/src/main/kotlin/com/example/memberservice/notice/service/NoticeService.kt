package com.example.memberservice.notice.service

import com.example.memberservice.member.repository.MemberRepository
import com.example.memberservice.notice.client.PushFeignClient
import com.example.memberservice.notice.dto.CreateNoticeDto
import com.example.memberservice.notice.dto.NoticeDto
import com.example.memberservice.notice.dto.NoticeWriter
import com.example.memberservice.notice.dto.PushMessageDto
import com.example.memberservice.notice.entity.Notice
import com.example.memberservice.notice.repository.NoticeRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val memberRepository: MemberRepository,
    private val pushFeignClient: PushFeignClient,
) {

    private val log = LoggerFactory.getLogger(NoticeService::class.java)

    /** 앱 공지사항 목록. 최신 글이 위로 온다. */
    @Transactional(readOnly = true)
    fun getNotices(): List<NoticeDto> = noticeRepository.findAllByOrderByIdDesc().map(NoticeDto::from)

    @Transactional(readOnly = true)
    fun searchNotices(pageable: Pageable): Page<NoticeDto> =
        noticeRepository.findAllByOrderByIdDesc(pageable).map(NoticeDto::from)

    @Transactional(readOnly = true)
    fun getNotice(id: Long): NoticeDto? = noticeRepository.findById(id).map(NoticeDto::from).orElse(null)

    /**
     * 공지를 저장하고, 요청이면 전체 푸시로도 내보낸다.
     * 푸시가 실패해도 공지는 남긴다 — 글이 사라지는 것보다 알림이 못 간 편이 낫고,
     * 알림을 놓쳐도 사용자가 공지사항에서 다시 볼 수 있다.
     *
     * @param writerUserId 게이트웨이가 관리자 JWT 를 검증하고 넣어 주는 userId.
     *                     클라이언트가 직접 채울 수 없는 값이라 작성자를 이걸로 정한다.
     */
    @Transactional
    fun createNotice(request: CreateNoticeDto, writerUserId: String?): NoticeDto {
        val saved = noticeRepository.save(
            Notice(request.title!!, request.content!!, writerUserId, resolveWriterName(writerUserId)),
        )

        if (request.push) {
            try {
                pushFeignClient.broadcast(
                    PushMessageDto(
                        request.title, request.content,
                        mapOf("type" to "notice", "noticeId" to saved.id.toString()), null,
                    ),
                )
            } catch (e: Exception) {
                log.warn("공지 {} 저장은 됐지만 푸시 발송에 실패했다: {}", saved.id, e.message)
            }
        }

        return NoticeDto.from(saved)
    }

    /** 회원을 못 찾아도 저장은 막지 않는다. 이름 하나 때문에 공지가 안 올라가면 곤란하다. */
    private fun resolveWriterName(writerUserId: String?): String {
        if (!StringUtils.hasText(writerUserId)) {
            return NoticeWriter.DEFAULT_NAME
        }
        return memberRepository.findByUserId(writerUserId!!)
            .map { member -> if (StringUtils.hasText(member.username)) member.username!! else NoticeWriter.DEFAULT_NAME }
            .orElse(NoticeWriter.DEFAULT_NAME)
    }
}
