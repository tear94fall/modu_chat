package com.example.authservice.admin

import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.dto.MemberDto
import com.example.authservice.member.dto.Role
import feign.FeignException
import java.util.concurrent.ConcurrentHashMap
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

/**
 * 백오피스 로그인. 계정은 하나: 비밀번호는 modu.admin.password-hash(bcrypt) 와 비교하고,
 * 이메일은 member-service 에서 role 이 ROLE_ADMIN 인 회원이어야 한다. 실패 사유는 구분하지 않는다.
 *
 * 단, member-service 조회 자체가 안 된 경우(5xx, 커넥션 실패, Eureka 에 인스턴스가 없는 경우 등)는
 * 자격 증명이 틀린 것으로 보지 않는다: [AdminLoginException] 은 그대로 던지지만 실패 횟수는
 * 세지 않는다 — member-service 재시작 같은 백엔드 장애로 관리자 계정이 잠기는 것을 막기 위함이다.
 * 조회가 정상적으로 응답했는데 회원이 없는 경우(404)는 조회가 "성공"한 것으로 보고 실패로 센다.
 *
 * 로그인 시도 제한은 인스턴스 메모리 카운터라 다중 인스턴스에선 인스턴스별로 센다.
 */
@Service
class AdminLoginService(
    private val memberFeignClient: MemberFeignClient,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${modu.admin.password-hash:}") private val passwordHash: String,
) {

    class AdminLoginException : RuntimeException("admin login failed")

    /** 검증에 성공한 관리자. 토큰은 OAuth2 토큰 엔드포인트(admin_password grant)가 만든다. */
    data class AdminMember(val userId: String, val roles: List<String>)

    /** email → [count, windowStartMillis] */
    private val failures = ConcurrentHashMap<String, LongArray>()

    fun login(email: String?, password: String?): AdminMember {
        if (isLocked(email)) {
            throw AdminLoginException()
        }
        if (!StringUtils.hasText(passwordHash) || !StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw AdminLoginException()
        }
        var member: MemberDto? = null
        var lookupUnavailable = false
        try {
            member = memberFeignClient.getMemberByEmail(email!!)
        } catch (e: FeignException) {
            // 404 는 member-service 가 정상 응답한 것 -> 회원이 없을 뿐이니 실패로 센다.
            // 그 외(5xx, 타임아웃 등)는 조회 자체가 안 된 것이니 실패로 세지 않는다.
            lookupUnavailable = e.status() != 404
        } catch (e: RuntimeException) {
            // Eureka 에 인스턴스가 없는 경우 등 Feign 이 감싸지 않은 예외도 장애로 취급한다.
            // 여기서 바로 던지면 이메일 존재 여부가 응답 시간으로 새어나가니, 아래에서 항상 matches 를 실행한 뒤 던진다.
            lookupUnavailable = true
        }
        val passwordMatches = passwordEncoder.matches(password, passwordHash) // 어느 경로든 항상 실행
        if (lookupUnavailable) {
            throw AdminLoginException()
        }
        val isAdmin = member != null && member.role == Role.ROLE_ADMIN
        if (!passwordMatches || !isAdmin) {
            recordFailure(email!!)
            throw AdminLoginException()
        }
        clearFailures(email!!)
        return AdminMember(member!!.userId!!, listOf(Role.ROLE_ADMIN.roleName))
    }

    /** 창 안에서 실패가 MAX_FAILURES 이상이면 true. */
    internal fun isLocked(email: String?): Boolean {
        if (email == null) return false // ConcurrentHashMap 은 null 키를 허용하지 않는다. 아래 hasText 검증에서 401로 처리된다.
        val f = failures[email] ?: return false
        if (System.currentTimeMillis() - f[1] > WINDOW_MS) {
            failures.remove(email)
            return false
        }
        return f[0] >= MAX_FAILURES
    }

    internal fun recordFailure(email: String) {
        failures.compute(email) { _, f ->
            val now = System.currentTimeMillis()
            if (f == null || now - f[1] > WINDOW_MS) {
                longArrayOf(1, now)
            } else {
                f[0]++
                f
            }
        }
    }

    internal fun clearFailures(email: String) {
        failures.remove(email)
    }

    companion object {
        private const val MAX_FAILURES = 5
        private const val WINDOW_MS = 15 * 60 * 1000L
    }
}
