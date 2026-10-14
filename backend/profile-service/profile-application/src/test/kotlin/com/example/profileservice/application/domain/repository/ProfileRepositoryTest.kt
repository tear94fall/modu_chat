package com.example.profileservice.application.domain.repository

import com.example.profileservice.application.config.RwJpaConfig
import com.example.profileservice.application.domain.entity.Profile
import com.example.profileservice.application.domain.entity.ProfileType
import com.example.profileservice.application.domain.repository.ro.ProfileRoRepository
import com.example.profileservice.application.domain.repository.rw.ProfileRwRepository
import com.example.profileservice.application.port.MemberIdentity
import com.example.profileservice.application.port.StorageGateway
import com.example.profileservice.application.port.StorageRollbackPublisher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * kapt 가 만든 QProfile 로 QueryDSL 질의가 H2 에서 도는지 본다(Kotlin 전환의 핵심 위험 지점).
 * 컬럼 이름 `value` 가 H2 예약어라 테스트 yml 의 MODE=MYSQL 데이터소스를 그대로 써야 테이블이 만들어진다.
 * 쓰기는 rw 저장소, 조회는 ro 저장소로 한다(테스트에서는 둘이 같은 H2 를 본다).
 */
@SpringBootTest
class ProfileRepositoryTest {

    @Autowired lateinit var profileRwRepository: ProfileRwRepository
    @Autowired lateinit var profileRoRepository: ProfileRoRepository
    @Autowired @Qualifier(RwJpaConfig.TRANSACTION_MANAGER) lateinit var rwTransactionManager: PlatformTransactionManager
    @MockitoBean lateinit var storageGateway: StorageGateway
    @MockitoBean lateinit var memberIdentity: MemberIdentity
    @MockitoBean lateinit var storageRollbackPublisher: StorageRollbackPublisher

    @BeforeEach
    fun clear() {
        profileRwRepository.deleteAll()
    }

    private fun save(memberId: Long, type: ProfileType, value: String): Profile =
        profileRwRepository.saveAndFlush(Profile(memberId, type, value))

    private fun delete(memberId: Long, id: Long): Long =
        requireNotNull(TransactionTemplate(rwTransactionManager).execute { profileRwRepository.deleteByMemberProfile(memberId, id) })

    @Test
    fun queriesAreScopedToTheMember() {
        val a1 = save(1L, ProfileType.PROFILE_IMAGE, "a1.png")
        val a2 = save(1L, ProfileType.PROFILE_STATUS_MESSAGE, "hello")
        val b1 = save(2L, ProfileType.PROFILE_IMAGE, "b1.png")

        assertEquals(a1.id, profileRoRepository.findByMemberProfile(1L, a1.id!!)?.id)
        assertNull(profileRoRepository.findByMemberProfile(2L, a1.id!!))
        assertEquals(a1.id, profileRwRepository.findByMemberProfile(1L, a1.id!!)?.id)
        assertNull(profileRwRepository.findByMemberProfile(2L, a1.id!!))
        assertEquals(2L, profileRoRepository.findMemberTotalProfiles(1L))
        assertEquals(1L, profileRoRepository.findMemberTotalProfiles(2L))
        assertEquals(listOf(a1.id), profileRoRepository.findByMemberProfileOffset(1L, a2.id!!, 10L).map { it.id })
        assertEquals(b1.id, profileRoRepository.findLatestProfile(2L)?.id)
        assertEquals(listOf(a1.id, a2.id), profileRoRepository.findByMemberId(1L).map { it.id }.sortedBy { it })
    }

    @Test
    fun deleteRemovesOnlyThatMembersRow() {
        val a1 = save(1L, ProfileType.PROFILE_IMAGE, "a1.png")
        save(2L, ProfileType.PROFILE_IMAGE, "b1.png")

        assertEquals(0L, delete(2L, a1.id!!))
        assertEquals(1L, delete(1L, a1.id!!))
        assertEquals(1, profileRwRepository.findAll().size)
    }
}
