package com.example.memberservice.application.domain.repository.rw

import com.example.memberservice.application.config.RwRepository
import com.example.memberservice.application.domain.entity.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/** 공지 쓰기와, 백오피스가 등록 직후 다시 읽는 목록(master). */
interface NoticeRwRepository : RwRepository<Notice, Long> {

    fun findAllByOrderByIdDesc(pageable: Pageable): Page<Notice>
}
