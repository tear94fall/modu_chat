package com.example.chatservice.application.config

import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.Repository

/**
 * 읽기 전용(replica) 저장소 마커. 조회 메서드만 선언할 수 있게 [Repository] 만 잇는다(save/delete 가 없다).
 * 이 인터페이스 자체는 빈으로 등록하지 않는다.
 */
@NoRepositoryBean
interface RoRepository<T, ID> : Repository<T, ID>
