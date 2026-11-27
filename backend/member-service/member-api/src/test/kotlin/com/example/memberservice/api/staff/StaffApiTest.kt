package com.example.memberservice.api.staff

import com.example.memberservice.api.support.ApiTestSupport
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberServiceUsage
import com.example.memberservice.application.domain.entity.ModuService
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.domain.entity.Staff
import com.example.memberservice.application.domain.entity.StaffPermission
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberServiceUsageRwRepository
import com.example.memberservice.application.domain.repository.rw.StaffRwRepository
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

class StaffApiTest : ApiTestSupport() {

    private val mvc: MockMvc get() = mockMvc
    @Autowired lateinit var members: MemberRwRepository
    @Autowired lateinit var staffRepository: StaffRwRepository
    @Autowired lateinit var usages: MemberServiceUsageRwRepository

    private lateinit var boss: Member
    private lateinit var alice: Member
    private lateinit var bob: Member

    private fun member(userId: String, email: String, name: String) =
        members.save(Member(userId = userId, email = email, username = name, role = Role.ROLE_MEMBER))

    @BeforeEach
    fun setUp() {
        boss = member("u-boss", "boss@modu.dev", "대표")
        alice = member("u-alice", "alice@modu.dev", "앨리스")
        bob = member("u-bob", "bob@modu.dev", "밥")
        // 첫 최상위 관리자는 DB 에 직접 넣는다(누가 바꿨는지 없음).
        staffRepository.save(Staff(boss.id!!, setOf(StaffPermission.SUPER), null))
    }

    private fun putPermissions(target: Member, body: String, actor: String = "u-boss"): ResultActionsDsl =
        mvc.put("/api-super/staff/${target.id}") {
            header(TOKEN_HEADER, TOKEN)
            header("X-Auth-User-Id", actor)
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

    @Test
    fun `staff api needs the internal token`() {
        mvc.get("/api-super/member").andExpect { status { isForbidden() } }
        mvc.get("/api-staff/staff").andExpect { status { isForbidden() } }
    }

    @Test
    fun `super designates staff and the login lookup returns the permissions`() {
        putPermissions(alice, """{"permissions":["INTERNAL","ADMIN"]}""").andExpect {
            status { isOk() }
            jsonPath("$.userId") { value("u-alice") }
            jsonPath("$.permissions[0]") { value("ADMIN") }
            jsonPath("$.permissions[1]") { value("INTERNAL") }
            jsonPath("$.modifiedBy") { value("u-boss") }
            jsonPath("$.modifiedByName") { value("대표") }
        }

        mvc.get("/api-internal/staff/by-email/alice@modu.dev") { header(TOKEN_HEADER, TOKEN) }.andExpect {
            status { isOk() }
            jsonPath("$.userId") { value("u-alice") }
            jsonPath("$.permissions.length()") { value(2) }
        }
        mvc.get("/api-internal/staff/user/u-alice") { header(TOKEN_HEADER, TOKEN) }.andExpect { status { isOk() } }
    }

    @Test
    fun `changing permissions replaces them`() {
        putPermissions(alice, """{"permissions":["ADMIN","SYSTEM"]}""").andExpect { status { isOk() } }
        putPermissions(alice, """{"permissions":["SYSTEM"]}""").andExpect {
            status { isOk() }
            jsonPath("$.permissions.length()") { value(1) }
            jsonPath("$.permissions[0]") { value("SYSTEM") }
        }
    }

    @Test
    fun `non staff, removed staff and withdrawn members cannot log in`() {
        mvc.get("/api-internal/staff/by-email/bob@modu.dev") { header(TOKEN_HEADER, TOKEN) }.andExpect { status { isNotFound() } }

        putPermissions(alice, """{"permissions":["ADMIN"]}""").andExpect { status { isOk() } }
        mvc.delete("/api-super/staff/${alice.id}") {
            header(TOKEN_HEADER, TOKEN)
            header("X-Auth-User-Id", "u-boss")
        }.andExpect { status { isNoContent() } }
        mvc.get("/api-internal/staff/user/u-alice") { header(TOKEN_HEADER, TOKEN) }.andExpect { status { isNotFound() } }

        putPermissions(bob, """{"permissions":["ADMIN"]}""").andExpect { status { isOk() } }
        bob.withdraw()
        members.saveAndFlush(bob)
        mvc.get("/api-internal/staff/user/u-bob") { header(TOKEN_HEADER, TOKEN) }.andExpect { status { isNotFound() } }
        putPermissions(bob, """{"permissions":["ADMIN"]}""").andExpect { status { isConflict() } }
    }

    @Test
    fun `super cannot change or remove themselves`() {
        putPermissions(boss, """{"permissions":["ADMIN"]}""").andExpect {
            status { isConflict() }
            jsonPath("$.message") { value("자기 자신의 직원 권한은 바꿀 수 없습니다. 다른 최상위 관리자에게 요청하세요.") }
        }
        mvc.delete("/api-super/staff/${boss.id}") {
            header(TOKEN_HEADER, TOKEN)
            header("X-Auth-User-Id", "u-boss")
        }.andExpect { status { isConflict() } }
        assertEquals(setOf(StaffPermission.SUPER), staffRepository.findById(boss.id!!).get().permissions)
    }

    @Test
    fun `empty permissions, unknown member and missing actor are rejected`() {
        putPermissions(alice, """{"permissions":[]}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { exists() }
        }
        mvc.put("/api-super/staff/999999") {
            header(TOKEN_HEADER, TOKEN)
            header("X-Auth-User-Id", "u-boss")
            contentType = MediaType.APPLICATION_JSON
            content = """{"permissions":["ADMIN"]}"""
        }.andExpect { status { isNotFound() } }
        mvc.put("/api-super/staff/${alice.id}") {
            header(TOKEN_HEADER, TOKEN)
            contentType = MediaType.APPLICATION_JSON
            content = """{"permissions":["ADMIN"]}"""
        }.andExpect { status { isUnauthorized() } }
        assertFalse(staffRepository.existsById(alice.id!!))
    }

    @Test
    fun `member search shows permissions and can list staff only`() {
        putPermissions(alice, """{"permissions":["INTERNAL"]}""").andExpect { status { isOk() } }

        // 회원 검색·상세는 직원을 지정하려는 최상위만(/api-super) — 인터널 권한만으로는 회원 이메일을 뒤질 수 없다
        mvc.get("/api-staff/member") { header(TOKEN_HEADER, TOKEN) }.andExpect { status { isNotFound() } }
        mvc.get("/api-super/member") {
            header(TOKEN_HEADER, TOKEN)
            param("keyword", "modu.dev")
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalElements") { value(3) }
            jsonPath("$.content[?(@.userId == 'u-alice')].permissions[0]") { value("INTERNAL") }
            jsonPath("$.content[?(@.userId == 'u-bob')].permissions.length()") { value(0) }
        }
        // 사용자 ID 로도 찾는다(직원 추가 화면이 약속하는 것; 어드민 목록도 같이 좋아진다)
        mvc.get("/api-super/member") {
            header(TOKEN_HEADER, TOKEN)
            param("keyword", "u-ali")
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalElements") { value(1) }
            jsonPath("$.content[0].userId") { value("u-alice") }
        }
        mvc.get("/api-super/member") {
            header(TOKEN_HEADER, TOKEN)
            param("keyword", "modu.dev")
            param("staffOnly", "true")
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalElements") { value(2) }
        }
        mvc.get("/api-super/member/${alice.id}") { header(TOKEN_HEADER, TOKEN) }.andExpect {
            status { isOk() }
            jsonPath("$.staff.permissions[0]") { value("INTERNAL") }
            jsonPath("$.staff.modifiedByName") { value("대표") }
            jsonPath("$.friendCount") { value(0) }
        }
        mvc.get("/api-super/member/${bob.id}") { header(TOKEN_HEADER, TOKEN) }.andExpect {
            status { isOk() }
            jsonPath("$.staff") { value(null as Any?) }
        }
        // 직원 목록·직원 한 명은 인터널 권한(/api-staff)으로 읽는다. 옛 /api-super/staff GET 은 없다.
        mvc.get("/api-staff/staff") { header(TOKEN_HEADER, TOKEN) }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[?(@.userId == 'u-alice')].modifiedByName") { value("대표") }
        }
        mvc.get("/api-staff/staff/${alice.id}") { header(TOKEN_HEADER, TOKEN) }.andExpect {
            status { isOk() }
            jsonPath("$.memberId") { value(alice.id!!) }
            jsonPath("$.email") { value("alice@modu.dev") }
            jsonPath("$.permissions[0]") { value("INTERNAL") }
        }
        mvc.get("/api-staff/staff/${bob.id}") { header(TOKEN_HEADER, TOKEN) }.andExpect { status { isNotFound() } }
        mvc.get("/api-super/staff") { header(TOKEN_HEADER, TOKEN) }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `admin member summary counts active, withdrawn, staff and service usage of active members`() {
        putPermissions(alice, """{"permissions":["INTERNAL"]}""").andExpect { status { isOk() } }
        val gone = member("u-gone", "gone@modu.dev", "떠남").also { it.withdraw(); members.save(it) }
        usages.save(MemberServiceUsage("u-boss", ModuService.CHAT, LocalDateTime.now()))
        usages.save(MemberServiceUsage("u-boss", ModuService.COMMERCE, LocalDateTime.now()))
        usages.save(MemberServiceUsage("u-alice", ModuService.CHAT, LocalDateTime.now()))
        usages.save(MemberServiceUsage(gone.userId, ModuService.COMMERCE, LocalDateTime.now()))   // 탈퇴 회원의 이용 기록은 세지 않는다

        mvc.get("/api-admin/member/summary") { header(TOKEN_HEADER, TOKEN) }.andExpect {
            status { isOk() }
            jsonPath("$.active") { value(3) }
            jsonPath("$.withdrawn") { value(1) }
            jsonPath("$.staff") { value(2) }
            jsonPath("$.chat") { value(2) }
            jsonPath("$.commerce") { value(1) }
            jsonPath("$.both") { value(1) }
            jsonPath("$.none") { value(1) }
        }
    }

    @Test
    fun `admin member search and detail show staff permissions instead of the old role`() {
        putPermissions(alice, """{"permissions":["SYSTEM","ADMIN"]}""").andExpect { status { isOk() } }

        mvc.get("/api-admin/member") {
            header(TOKEN_HEADER, TOKEN)
            param("keyword", "modu.dev")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[?(@.userId == 'u-boss')].staffPermissions[0]") { value("SUPER") }
            jsonPath("$.content[?(@.userId == 'u-alice')].staffPermissions.length()") { value(2) }
            jsonPath("$.content[?(@.userId == 'u-bob')].staffPermissions.length()") { value(0) }
        }
        mvc.get("/api-admin/member/${alice.id}") { header(TOKEN_HEADER, TOKEN) }.andExpect {
            status { isOk() }
            jsonPath("$.staffPermissions[0]") { value("ADMIN") }
            jsonPath("$.staffPermissions[1]") { value("SYSTEM") }
        }
        mvc.get("/api-admin/member/me") {
            header(TOKEN_HEADER, TOKEN)
            header("X-Auth-User-Id", "u-boss")
        }.andExpect {
            status { isOk() }
            jsonPath("$.staffPermissions[0]") { value("SUPER") }
        }
    }

    companion object {
        private const val TOKEN_HEADER = "X-Internal-Token"
        private const val TOKEN = "test-internal-token"
    }
}
