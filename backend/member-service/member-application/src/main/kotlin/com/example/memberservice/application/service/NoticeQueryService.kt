package com.example.memberservice.application.service

import com.example.memberservice.application.config.RoJpaConfig
import com.example.memberservice.application.domain.repository.ro.NoticeRoRepository
import com.example.memberservice.application.usecase.result.NoticeResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 앱 설정 > 공지사항(replica). */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class NoticeQueryService(private val noticeRoRepository: NoticeRoRepository) {

    /** 앱 공지사항 목록. 최신 글이 위로 온다. */
    fun getNotices(): List<NoticeResult> = noticeRoRepository.findAllByOrderByIdDesc().map(NoticeResult::from)

    fun getNotice(id: Long): NoticeResult? = noticeRoRepository.findById(id).map(NoticeResult::from).orElse(null)
}
