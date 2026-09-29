package com.example.profileservice.application.usecase

import com.example.profileservice.application.common.exception.CustomException
import com.example.profileservice.application.common.exception.ErrorCode
import com.example.profileservice.application.domain.entity.ProfileType
import com.example.profileservice.application.domain.repository.rw.ProfileRwRepository
import com.example.profileservice.application.port.MemberIdentity
import com.example.profileservice.application.port.StorageGateway
import com.example.profileservice.application.port.StorageRollbackPublisher
import com.example.profileservice.application.usecase.command.CreateProfileCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class ProfileUseCaseTest {

    @Autowired lateinit var profileUseCase: ProfileUseCase
    @Autowired lateinit var profileRwRepository: ProfileRwRepository
    @MockitoBean lateinit var storageGateway: StorageGateway
    @MockitoBean lateinit var memberIdentity: MemberIdentity
    @MockitoBean lateinit var storageRollbackPublisher: StorageRollbackPublisher

    @BeforeEach
    fun setUp() {
        profileRwRepository.deleteAll()
        whenever(memberIdentity.memberIdOf("user-1")).thenReturn(1L)
        whenever(memberIdentity.memberIdOf("user-2")).thenReturn(2L)
    }

    @Test
    fun register_thenRead() {
        val saved = profileUseCase.registerMine("user-1", CreateProfileCommand(1L, ProfileType.PROFILE_IMAGE, "a.png"))

        assertThat(saved.id).isNotNull()
        assertThat(saved.createdDate).isNotNull()
        assertThat(profileUseCase.profile("1", saved.id.toString()).value).isEqualTo("a.png")
        assertThat(profileUseCase.latest("1").id).isEqualTo(saved.id)
        assertThat(profileUseCase.totalCount("1")).isEqualTo(1L)
        assertThat(profileUseCase.profiles(1L)).hasSize(1)
        assertThat(profileUseCase.profilesFresh(1L)).hasSize(1)
        verify(storageRollbackPublisher, never()).publish(any(), any())
    }

    @Test
    fun register_forSomeoneElse_isForbidden() {
        val e = assertThrows<CustomException> {
            profileUseCase.registerMine("user-2", CreateProfileCommand(1L, ProfileType.PROFILE_IMAGE, "a.png"))
        }
        assertThat(e.errorCode).isEqualTo(ErrorCode.FORBIDDEN)
        assertThat(profileRwRepository.count()).isZero()
    }

    @Test
    fun missingProfile_isNotFound() {
        assertThat(assertThrows<CustomException> { profileUseCase.profile("1", "999") }.errorCode).isEqualTo(ErrorCode.PROFILE_NOT_FOUND)
        assertThat(assertThrows<CustomException> { profileUseCase.latest("1") }.errorCode).isEqualTo(ErrorCode.PROFILE_NOT_FOUND)
        assertThat(assertThrows<CustomException> { profileUseCase.deleteMine("user-1", "1", "999") }.errorCode)
            .isEqualTo(ErrorCode.PROFILE_NOT_FOUND)
        assertThrows<NumberFormatException> { profileUseCase.latest("abc") }
    }

    @Test
    fun delete_image_removesFileThenRow_statusMessageKeepsStorageUntouched() {
        val image = profileUseCase.register(CreateProfileCommand(1L, ProfileType.PROFILE_IMAGE, "a.png"))
        val message = profileUseCase.register(CreateProfileCommand(1L, ProfileType.PROFILE_STATUS_MESSAGE, "hello"))

        assertThat(profileUseCase.deleteMine("user-1", "1", message.id.toString())).isEqualTo(1L)
        verify(storageGateway, never()).delete(any())

        assertThat(profileUseCase.deleteMine("user-1", "1", image.id.toString())).isEqualTo(1L)
        verify(storageGateway).delete("a.png")
        assertThat(profileRwRepository.count()).isZero()
    }

    @Test
    fun delete_someoneElsesProfile_isForbidden_andNothingIsRemoved() {
        val image = profileUseCase.register(CreateProfileCommand(1L, ProfileType.PROFILE_IMAGE, "a.png"))

        val e = assertThrows<CustomException> { profileUseCase.deleteMine("user-2", "1", image.id.toString()) }

        assertThat(e.errorCode).isEqualTo(ErrorCode.FORBIDDEN)
        verify(storageGateway, never()).delete(any())
        assertThat(profileRwRepository.count()).isEqualTo(1L)
    }

    @Test
    fun delete_whenStorageFails_keepsTheRow() {
        val image = profileUseCase.register(CreateProfileCommand(1L, ProfileType.PROFILE_IMAGE, "a.png"))
        whenever(storageGateway.delete("a.png")).thenThrow(IllegalStateException("storage down"))

        assertThrows<IllegalStateException> { profileUseCase.deleteMine("user-1", "1", image.id.toString()) }

        assertThat(profileRwRepository.count()).isEqualTo(1L)
    }
}
