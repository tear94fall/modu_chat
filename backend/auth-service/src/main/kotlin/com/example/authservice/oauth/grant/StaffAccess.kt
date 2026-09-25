package com.example.authservice.oauth.grant

import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.dto.StaffLoginDto
import feign.FeignException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes

/**
 * 직원 콘솔 토큰의 규칙. 직원 권한 → 토큰 roles, 그리고 member-service 조회.
 *
 * - SUPER(최상위 관리자)는 모든 콘솔을 쓴다: ROLE_SUPER + ROLE_ADMIN + ROLE_SYSTEM + ROLE_INTERNAL.
 * - 나머지는 ROLE_<권한> 하나씩(ADMIN → ROLE_ADMIN …). 게이트웨이가 콘솔별 경로를 이 role 로 지킨다.
 */
object StaffAccess {

    const val NOT_STAFF = "직원 계정이 아닙니다. 최상위 관리자에게 직원 등록을 요청하세요."

    private val CONSOLE_ROLES = listOf("ROLE_ADMIN", "ROLE_SYSTEM", "ROLE_INTERNAL")

    fun rolesOf(permissions: Collection<String>): List<String> {
        if ("SUPER" in permissions) return listOf("ROLE_SUPER") + CONSOLE_ROLES
        return CONSOLE_ROLES.filter { it.removePrefix("ROLE_") in permissions }
    }

    /**
     * 직원이면 정보, 직원이 아니면(404) invalid_grant. member-service 가 응답하지 못하면 server_error 로 둔다 —
     * 장애를 "직원이 아님"으로 보이게 하지 않는다.
     */
    fun lookup(find: () -> StaffLoginDto): StaffLoginDto {
        val staff = try {
            find()
        } catch (e: FeignException.NotFound) {
            throw notStaff()
        } catch (e: RuntimeException) {
            throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "직원 정보를 확인하지 못했습니다. 잠시 뒤 다시 시도하세요.", null))
        }
        if (staff.userId.isNullOrBlank() || rolesOf(staff.permissions).isEmpty()) throw notStaff()
        return staff
    }

    fun byEmail(members: MemberFeignClient, email: String) = lookup { members.staffByEmail(email) }

    fun byUserId(members: MemberFeignClient, userId: String) = lookup { members.staffByUserId(userId) }

    fun notStaff() = OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, NOT_STAFF, null))
}
