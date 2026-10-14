package com.example.memberservice.application.domain.repository.ro

import com.example.memberservice.application.config.RoRepository
import com.example.memberservice.application.domain.entity.Notice
import java.util.Optional

/** 앱의 공지사항 읽기(replica). */
interface NoticeRoRepository : RoRepository<Notice, Long> {

    fun findById(id: Long): Optional<Notice>

    fun findAllByOrderByIdDesc(): List<Notice>
}
