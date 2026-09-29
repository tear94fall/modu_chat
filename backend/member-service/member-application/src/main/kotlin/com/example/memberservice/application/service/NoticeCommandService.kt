package com.example.memberservice.application.service

import com.example.memberservice.application.config.RwJpaConfig
import com.example.memberservice.application.domain.entity.Notice
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.domain.repository.rw.NoticeRwRepository
import com.example.memberservice.application.usecase.command.CreateNoticeCommand
import com.example.memberservice.application.usecase.result.NoticeResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils

/** 공지 쓰기(master)와, 백오피스가 등록 직후 다시 읽는 목록. 푸시 발송은 유스케이스가 트랜잭션 밖에서 한다. */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class NoticeCommandService(
    private val noticeRwRepository: NoticeRwRepository,
    private val memberRwRepository: MemberRwRepository,
) {

    /** 백오피스 목록. 등록 직후에 다시 읽으므로 master 에서 읽는다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun searchNotices(pageable: Pageable): Page<NoticeResult> =
        noticeRwRepository.findAllByOrderByIdDesc(pageable).map(NoticeResult::from)

    /**
     * 공지를 저장한다. writerUserId 는 게이트웨이가 관리자 JWT 를 검증하고 넣어 주는 userId —
     * 클라이언트가 직접 채울 수 없는 값이라 작성자를 이걸로 정한다.
     */
    fun createNotice(command: CreateNoticeCommand): NoticeResult =
        NoticeResult.from(
            noticeRwRepository.save(
                Notice(command.title, command.content, command.writerUserId, resolveWriterName(command.writerUserId)),
            ),
        )

    /** 회원을 못 찾아도 저장은 막지 않는다. 이름 하나 때문에 공지가 안 올라가면 곤란하다. */
    private fun resolveWriterName(writerUserId: String?): String {
        if (!StringUtils.hasText(writerUserId)) {
            return NoticeWriter.DEFAULT_NAME
        }
        return memberRwRepository.findByUserId(writerUserId!!)
            .map { member -> if (StringUtils.hasText(member.username)) member.username!! else NoticeWriter.DEFAULT_NAME }
            .orElse(NoticeWriter.DEFAULT_NAME)
    }
}
