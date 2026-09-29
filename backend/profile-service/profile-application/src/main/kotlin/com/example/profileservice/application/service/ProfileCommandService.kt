package com.example.profileservice.application.service

import com.example.profileservice.application.common.exception.CustomException
import com.example.profileservice.application.common.exception.ErrorCode
import com.example.profileservice.application.config.RwJpaConfig
import com.example.profileservice.application.domain.entity.Profile
import com.example.profileservice.application.domain.repository.rw.ProfileRwRepository
import com.example.profileservice.application.usecase.command.CreateProfileCommand
import com.example.profileservice.application.usecase.result.ProfileResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 프로필 기록 쓰기와, 쓰기 직후·쓰기 전에 봐야 하는 읽기(master). */
@Service
@Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER)
class ProfileCommandService(private val profileRwRepository: ProfileRwRepository) {

    fun register(command: CreateProfileCommand): ProfileResult =
        ProfileResult.from(profileRwRepository.save(Profile(command.memberId, command.profileType, command.value)))

    /**
     * member-service 가 기록을 추가한 바로 뒤에 회원 정보에 붙이려고 읽는다 — 레플리카에는 아직 없을 수 있어 master 에서 읽는다.
     */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun profilesFresh(memberId: Long): List<ProfileResult> = profileRwRepository.findByMemberId(memberId).map { ProfileResult.from(it) }

    /** 지우기 전에 대상이 이 회원의 것인지, 파일이 딸려 있는지 본다. */
    @Transactional(transactionManager = RwJpaConfig.TRANSACTION_MANAGER, readOnly = true)
    fun profileForDelete(memberId: Long, id: Long): ProfileResult =
        profileRwRepository.findByMemberProfile(memberId, id)?.let { ProfileResult.from(it) }
            ?: throw CustomException(ErrorCode.PROFILE_NOT_FOUND, "member $memberId, profile $id")

    /** 지운 행 수. */
    fun delete(memberId: Long, id: Long): Long = profileRwRepository.deleteByMemberProfile(memberId, id)
}
