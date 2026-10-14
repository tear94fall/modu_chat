package com.example.memberservice.application.domain.repository.rw

import com.example.memberservice.application.config.RwRepository
import com.example.memberservice.application.domain.entity.CommonData

/** 공통 설정 쓰기와, 백오피스가 저장 직후 다시 읽는 조회(master). */
interface CommonDataRwRepository : RwRepository<CommonData, String>
