package com.example.authservice.admin

import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.dto.MemberDto
import com.example.authservice.member.dto.Role
import feign.FeignException
import feign.Request
import feign.Response
import feign.RetryableException
import java.nio.charset.StandardCharsets
import java.util.Collections
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class AdminLoginServiceTest {

    private val encoder = BCryptPasswordEncoder()
    private val hash: String = encoder.encode("correct-pw")
    private val members: MemberFeignClient = mock()

    private fun service(configuredHash: String) = AdminLoginService(members, encoder, configuredHash)

    private fun member(role: Role) = MemberDto(userId = "admin-1", email = "admin@example.com", role = role)

    @Test
    fun success_returnsAdminIdentity() {
        whenever(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN))

        val admin = service(hash).login("admin@example.com", "correct-pw")

        assertEquals("admin-1", admin.userId)
        assertEquals(listOf("ROLE_ADMIN"), admin.roles)
    }

    @Test
    fun wrongPassword_throws() {
        whenever(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN))
        assertThrows(AdminLoginService.AdminLoginException::class.java) { service(hash).login("admin@example.com", "nope") }
    }

    @Test
    fun nonAdminMember_throws() {
        whenever(members.getMemberByEmail("user@example.com")).thenReturn(member(Role.ROLE_MEMBER))
        assertThrows(AdminLoginService.AdminLoginException::class.java) { service(hash).login("user@example.com", "correct-pw") }
    }

    @Test
    fun blankConfiguredHash_alwaysFails() {
        whenever(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN))
        assertThrows(AdminLoginService.AdminLoginException::class.java) { service("").login("admin@example.com", "correct-pw") }
    }

    @Test
    fun unknownEmail_throws() {
        whenever(members.getMemberByEmail("ghost@example.com")).thenThrow(RuntimeException("404"))
        assertThrows(AdminLoginService.AdminLoginException::class.java) { service(hash).login("ghost@example.com", "correct-pw") }
    }

    @Test
    fun blankPassword_throws() {
        whenever(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN))
        assertThrows(AdminLoginService.AdminLoginException::class.java) { service(hash).login("admin@example.com", " ") }
    }

    @Test
    fun sixthFailureWithinWindow_isLockedEvenWithCorrectPassword() {
        val s = service(hash)
        whenever(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN))
        repeat(5) {
            assertThrows(AdminLoginService.AdminLoginException::class.java) { s.login("admin@example.com", "nope") }
        }
        assertThrows(AdminLoginService.AdminLoginException::class.java) { s.login("admin@example.com", "correct-pw") }
    }

    private fun dummyRequest(): Request = Request.create(
        Request.HttpMethod.GET, "/api-internal/member/by-email/admin@example.com",
        Collections.emptyMap(), null, StandardCharsets.UTF_8,
    )

    private fun connectTimedOut() = RetryableException(-1, "connect timed out", Request.HttpMethod.GET, null as Long?, dummyRequest())

    @Test
    fun lookupUnavailable_doesNotCountTowardLock() {
        val s = service(hash)
        // 처음 다섯 번은 member-service 장애(커넥션 실패)로 조회 자체가 안 되는 상황.
        whenever(members.getMemberByEmail("admin@example.com"))
            .thenThrow(connectTimedOut())
            .thenThrow(RuntimeException("connection refused"))
            .thenThrow(connectTimedOut())
            .thenThrow(RuntimeException("connection refused"))
            .thenThrow(connectTimedOut())
        repeat(5) {
            assertThrows(AdminLoginService.AdminLoginException::class.java) { s.login("admin@example.com", "correct-pw") }
        }

        // member-service 가 복구되면 정상 로그인이 되어야 한다 -> 위 5번은 잠금에 반영되지 않았다.
        // (직전 스텁이 계속 던지는 상태라 whenever(mock.foo())...로 재스텁하면 그 인자 평가 자체가 던져버리니 doReturn 을 쓴다)
        doReturn(member(Role.ROLE_ADMIN)).whenever(members).getMemberByEmail("admin@example.com")

        val admin = s.login("admin@example.com", "correct-pw")
        assertEquals("admin-1", admin.userId)
    }

    @Test
    fun unknownEmail404_countsTowardLock() {
        val s = service(hash)
        val notFound: FeignException = FeignException.errorStatus(
            "getMemberByEmail",
            Response.builder()
                .status(404)
                .reason("Not Found")
                .request(dummyRequest())
                .headers(Collections.emptyMap())
                .build(),
        )
        whenever(members.getMemberByEmail("admin@example.com")).thenThrow(notFound)
        repeat(5) {
            assertThrows(AdminLoginService.AdminLoginException::class.java) { s.login("admin@example.com", "correct-pw") }
        }

        // 404 는 조회가 정상적으로 응답한 것이라 실패로 세어졌어야 하고, 실제 회원이 있어도 잠긴 채여야 한다.
        doReturn(member(Role.ROLE_ADMIN)).whenever(members).getMemberByEmail("admin@example.com")
        assertThrows(AdminLoginService.AdminLoginException::class.java) { s.login("admin@example.com", "correct-pw") }
    }
}
