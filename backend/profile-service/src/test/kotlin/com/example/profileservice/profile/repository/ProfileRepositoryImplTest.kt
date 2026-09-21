package com.example.profileservice.profile.repository

import com.example.profileservice.config.QuerydslConfig
import com.example.profileservice.profile.entity.Profile
import com.example.profileservice.profile.entity.ProfileType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * kapt 가 만든 QProfile 로 QueryDSL 질의가 H2 에서 도는지 본다(Kotlin 전환의 핵심 위험 지점).
 * 컬럼 이름 `value` 가 H2 예약어라 테스트 yml 의 MODE=MYSQL 데이터소스를 그대로 써야 테이블이 만들어진다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuerydslConfig::class)
class ProfileRepositoryImplTest {

    @Autowired lateinit var profileRepository: ProfileRepository

    private fun save(memberId: Long, type: ProfileType, value: String): Profile =
        profileRepository.saveAndFlush(Profile(memberId, type, value))

    @Test
    fun queriesAreScopedToTheMember() {
        val a1 = save(1L, ProfileType.PROFILE_IMAGE, "a1.png")
        val a2 = save(1L, ProfileType.PROFILE_STATUS_MESSAGE, "hello")
        val b1 = save(2L, ProfileType.PROFILE_IMAGE, "b1.png")

        assertEquals(a1.id, profileRepository.findByMemberProfile(1L, a1.id!!)?.id)
        assertNull(profileRepository.findByMemberProfile(2L, a1.id!!))
        assertEquals(2L, profileRepository.findMemberTotalProfiles(1L))
        assertEquals(1L, profileRepository.findMemberTotalProfiles(2L))
        assertEquals(listOf(a1.id), profileRepository.findByMemberProfileOffset(1L, a2.id!!, 10L).map { it.id })
        assertEquals(b1.id, profileRepository.findLatestProfile(2L)?.id)
    }

    @Test
    fun deleteRemovesOnlyThatMembersRow() {
        val a1 = save(1L, ProfileType.PROFILE_IMAGE, "a1.png")
        save(2L, ProfileType.PROFILE_IMAGE, "b1.png")

        assertEquals(0L, profileRepository.deleteByMemberProfile(2L, a1.id!!))
        assertEquals(1L, profileRepository.deleteByMemberProfile(1L, a1.id!!))
        assertEquals(1, profileRepository.findAll().size)
    }
}
