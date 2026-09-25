package com.example.authservice.oauth.grant.google

import com.example.authservice.oauth.grant.StaffAccess
import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.dto.GoogleAccountDto
import com.example.authservice.member.dto.MemberDto
import com.example.authservice.member.dto.Role
import com.example.authservice.oauth.config.AuthorizationServerConfig
import com.example.authservice.oauth.google.GoogleIdTokenVerifierService
import com.example.authservice.oauth.grant.GrantSupport
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken

/**
 * 구글 ID 토큰 → 회원 찾기/만들기 → 모두 토큰. 누가 이 사람인지 증명하는 것은 구글 서명뿐이다.
 *
 * 직원 콘솔 클라이언트([staffClientIds])는 다르다: 회원을 만들지 않고, 확인된 이메일의 회원이 직원일 때만
 * 직원 권한을 roles 로 담아 발급한다([StaffAccess]).
 */
class GoogleIdTokenGrantProvider(
    private val verifier: GoogleIdTokenVerifierService,
    private val members: MemberFeignClient,
    private val grantSupport: GrantSupport,
    private val staffClientIds: Set<String> = emptySet(),
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val grant = authentication as GoogleIdTokenGrantToken
        val clientPrincipal = grant.principal as OAuth2ClientAuthenticationToken
        val rc = clientPrincipal.registeredClient
        if (rc == null || !rc.authorizationGrantTypes.contains(AuthorizationServerConfig.GOOGLE_ID_TOKEN)) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
        }
        val account = verifier.verify(grant.idToken)
        if (rc.clientId in staffClientIds) {
            if (!account.emailVerified) throw StaffAccess.notStaff()
            val staff = StaffAccess.byEmail(members, account.email)
            return grantSupport.issue(
                rc, clientPrincipal, AuthorizationServerConfig.GOOGLE_ID_TOKEN, staff.userId!!,
                StaffAccess.rolesOf(staff.permissions), GrantSupport.resolveScopes(rc, grant.scopes), grant,
            )
        }
        val member = members.googleMember(GoogleAccountDto(account.sub, account.email, account.name, account.picture))
        return grantSupport.issue(
            rc, clientPrincipal, AuthorizationServerConfig.GOOGLE_ID_TOKEN, member.userId!!,
            rolesOf(member), GrantSupport.resolveScopes(rc, grant.scopes), grant,
        )
    }

    override fun supports(authentication: Class<*>): Boolean =
        GoogleIdTokenGrantToken::class.java.isAssignableFrom(authentication)

    companion object {
        internal fun rolesOf(member: MemberDto): List<String> =
            listOf(if (member.role == Role.ROLE_ADMIN) "ROLE_ADMIN" else "ROLE_USER")
    }
}
