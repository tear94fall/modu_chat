package com.example.memberservice.notice.repository

import com.example.memberservice.notice.entity.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NoticeRepository : JpaRepository<Notice, Long> {

    fun findAllByOrderByIdDesc(): List<Notice>

    fun findAllByOrderByIdDesc(pageable: Pageable): Page<Notice>
}
