package com.example.profileservice.application.service

import com.example.profileservice.application.common.exception.CustomException
import com.example.profileservice.application.common.exception.ErrorCode
import com.example.profileservice.application.config.RoJpaConfig
import com.example.profileservice.application.domain.repository.ro.ProfileRoRepository
import com.example.profileservice.application.usecase.result.ProfileResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 앱의 프로필 둘러보기(replica). 복제가 따라오기 전에는 방금 쓴 기록이 안 보일 수 있다. */
@Service
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
class ProfileQueryService(private val profileRoRepository: ProfileRoRepository) {

    fun profiles(memberId: Long): List<ProfileResult> = profileRoRepository.findByMemberId(memberId).map { ProfileResult.from(it) }

    fun profile(memberId: Long, id: Long): ProfileResult =
        profileRoRepository.findByMemberProfile(memberId, id)?.let { ProfileResult.from(it) }
            ?: throw CustomException(ErrorCode.PROFILE_NOT_FOUND, "member $memberId, profile $id")

    fun latest(memberId: Long): ProfileResult =
        profileRoRepository.findLatestProfile(memberId)?.let { ProfileResult.from(it) }
            ?: throw CustomException(ErrorCode.PROFILE_NOT_FOUND, "member $memberId")

    fun olderThan(memberId: Long, id: Long, count: Long): List<ProfileResult> =
        profileRoRepository.findByMemberProfileOffset(memberId, id, count).map { ProfileResult.from(it) }

    fun totalCount(memberId: Long): Long = profileRoRepository.findMemberTotalProfiles(memberId)
}
