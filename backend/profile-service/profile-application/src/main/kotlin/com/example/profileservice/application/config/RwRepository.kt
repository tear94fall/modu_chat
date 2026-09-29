package com.example.profileservice.application.config

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

/** 읽기/쓰기(master) 저장소 마커. */
@NoRepositoryBean
interface RwRepository<T, ID> : JpaRepository<T, ID>
