package com.example.profileservice.profile.service

import com.example.profileservice.kafka.producer.KafkaProducerService
import com.example.profileservice.member.client.MemberFeignClient
import com.example.profileservice.profile.dto.CreateProfileDto
import com.example.profileservice.profile.dto.ProfileDto
import com.example.profileservice.profile.entity.Profile
import com.example.profileservice.profile.entity.ProfileType
import com.example.profileservice.profile.repository.ProfileRepository
import com.example.profileservice.storage.client.StorageFeignClient
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val memberFeignClient: MemberFeignClient,
    private val storageFeignClient: StorageFeignClient,
    private val kafkaProducerService: KafkaProducerService,
) {

    private val log = LoggerFactory.getLogger(ProfileService::class.java)

    @CircuitBreaker(name = "memberProfileCircuitBreaker", fallbackMethod = "fallbackGetMemberProfile")
    fun getMemberProfiles(memberId: Long): List<ProfileDto> = profileRepository.findByMemberId(memberId).map { ProfileDto(it) }

    /** resilience4j 가 이름으로 찾는다. 시그니처는 원본 + Throwable. */
    @Suppress("unused")
    private fun fallbackGetMemberProfile(memberId: Long, e: Throwable): List<ProfileDto> {
        log.info("fallbackGetMemberProfile Method Running and Error is " + e.message)
        return ArrayList()
    }

    fun getMemberLatestProfile(memberId: String): ProfileDto {
        val latestProfile = profileRepository.findLatestProfile(memberId.toLong()) ?: throw NoSuchElementException("no profile for member $memberId")
        return ProfileDto(latestProfile)
    }

    fun getMemberProfileOffset(memberId: String, id: String, count: String): List<ProfileDto> =
        profileRepository.findByMemberProfileOffset(memberId.toLong(), id.toLong(), count.toLong()).map { ProfileDto(it) }

    fun getMemberProfile(memberId: String, id: String): ProfileDto {
        val profile = profileRepository.findByMemberProfile(memberId.toLong(), id.toLong()) ?: throw NoSuchElementException("no profile $id for member $memberId")
        return ProfileDto(profile)
    }

    fun getMemberProfileTotalCount(memberId: String): Long = profileRepository.findMemberTotalProfiles(memberId.toLong())

    fun registerProfile(createProfileDto: CreateProfileDto): ProfileDto {
        val profile = Profile(createProfileDto)
        try {
            profileRepository.save(profile)
        } catch (e: Exception) {
            log.error(e.message)
            kafkaProducerService.sendMessage(profile.memberId?.toString(), ProfileDto(profile))
        }
        return ProfileDto(profile)
    }

    fun deleteProfile(memberId: String, id: String): Long {
        val findProfile = profileRepository.findByMemberProfile(memberId.toLong(), id.toLong()) ?: throw NoSuchElementException("no profile $id for member $memberId")
        if (findProfile.profileType != ProfileType.PROFILE_STATUS_MESSAGE) {
            findProfile.value?.let { storageFeignClient.delete(it) }
        }
        return profileRepository.deleteByMemberProfile(memberId.toLong(), id.toLong())
    }
}
