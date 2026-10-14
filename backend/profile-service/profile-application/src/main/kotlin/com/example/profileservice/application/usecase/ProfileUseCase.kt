package com.example.profileservice.application.usecase

import com.example.profileservice.application.common.exception.CustomException
import com.example.profileservice.application.common.exception.ErrorCode
import com.example.profileservice.application.domain.entity.ProfileType
import com.example.profileservice.application.port.MemberIdentity
import com.example.profileservice.application.port.StorageGateway
import com.example.profileservice.application.port.StorageRollbackPublisher
import com.example.profileservice.application.service.ProfileCommandService
import com.example.profileservice.application.service.ProfileQueryService
import com.example.profileservice.application.usecase.command.CreateProfileCommand
import com.example.profileservice.application.usecase.result.ProfileResult
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 프로필 기록 흐름. DB 트랜잭션은 서비스가 열고 닫으며, 다른 서비스 호출(storage·member)과 Kafka 발행은 여기서 트랜잭션 밖에서 한다.
 */
@Component
class ProfileUseCase(
    private val profileQueryService: ProfileQueryService,
    private val profileCommandService: ProfileCommandService,
    private val storageGateway: StorageGateway,
    private val memberIdentity: MemberIdentity,
    private val storageRollbackPublisher: StorageRollbackPublisher,
) {

    private val log = LoggerFactory.getLogger(ProfileUseCase::class.java)

    /** 앱의 기록 전체 조회(replica). DB 오류로 서킷 브레이커가 열리면 빈 목록. */
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "fallbackProfiles")
    fun profiles(memberId: Long): List<ProfileResult> = profileQueryService.profiles(memberId)

    /** member-service 가 부르는 기록 전체 조회(master — 추가 직후에 읽는다). DB 오류로 서킷 브레이커가 열리면 빈 목록. */
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "fallbackProfiles")
    fun profilesFresh(memberId: Long): List<ProfileResult> = profileCommandService.profilesFresh(memberId)

    /** resilience4j 가 이름으로 찾는다. 시그니처는 원본 + Throwable. */
    @Suppress("unused")
    private fun fallbackProfiles(memberId: Long, e: Throwable): List<ProfileResult> {
        log.info("fallbackProfiles Method Running and Error is " + e.message)
        return ArrayList()
    }

    fun profile(memberId: String, id: String): ProfileResult = profileQueryService.profile(memberId.toLong(), id.toLong())

    fun latest(memberId: String): ProfileResult = profileQueryService.latest(memberId.toLong())

    fun olderThan(memberId: String, id: String, count: String): List<ProfileResult> =
        profileQueryService.olderThan(memberId.toLong(), id.toLong(), count.toLong())

    fun totalCount(memberId: String): Long = profileQueryService.totalCount(memberId.toLong())

    /** 앱이 부르는 추가: 본문의 memberId 가 로그인한 본인이어야 한다. */
    fun registerMine(authUserId: String, command: CreateProfileCommand): ProfileResult {
        requireSelf(authUserId, command.memberId)
        return register(command)
    }

    /** DB 저장에 실패하면 올려 둔 파일을 되돌리라고 Kafka 로 알리고, id 없는 값을 돌려준다(옛 동작 그대로). */
    fun register(command: CreateProfileCommand): ProfileResult =
        try {
            profileCommandService.register(command)
        } catch (e: Exception) {
            log.error(e.message)
            val unsaved = ProfileResult(null, command.memberId, command.profileType, command.value, null, null)
            storageRollbackPublisher.publish(command.memberId?.toString(), unsaved)
            unsaved
        }

    /** 앱이 부르는 삭제: 경로의 memberId 가 로그인한 본인이어야 한다. 사진·배경이면 파일을 먼저 지운다. 지운 행 수를 돌려준다. */
    fun deleteMine(authUserId: String, memberId: String, id: String): Long {
        val memberPk = memberId.toLong()
        val profileId = id.toLong()
        requireSelf(authUserId, memberPk)
        val target = profileCommandService.profileForDelete(memberPk, profileId)
        if (target.profileType != ProfileType.PROFILE_STATUS_MESSAGE) {
            target.value?.let { storageGateway.delete(it) }
        }
        return profileCommandService.delete(memberPk, profileId)
    }

    private fun requireSelf(authUserId: String, memberId: Long?) {
        if (memberId == null || memberIdentity.memberIdOf(authUserId) != memberId) throw CustomException(ErrorCode.FORBIDDEN)
    }

    companion object {
        const val CIRCUIT_BREAKER = "memberProfileCircuitBreaker"
    }
}
