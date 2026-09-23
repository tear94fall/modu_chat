package com.example.pointservice.point.repository

import com.example.pointservice.point.entity.PointAccount
import jakarta.persistence.LockModeType
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PointAccountRepository : JpaRepository<PointAccount, Long> {

    fun findByUserId(userId: String): Optional<PointAccount>

    /** 잔액을 바꿀 때는 행을 잠근다. 같은 사용자의 적립·사용이 동시에 와도 잔액이 어긋나지 않는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PointAccount a where a.userId = :userId")
    fun findByUserIdForUpdate(userId: String): Optional<PointAccount>

    fun findByUserIdIn(userIds: Collection<String>, pageable: Pageable): Page<PointAccount>
}
