package com.example.memberservice.api.admin

import com.example.memberservice.api.support.ApiTestSupport
import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.MemberFriend
import com.example.memberservice.application.domain.repository.rw.MemberFriendRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 백오피스 회원 상세 친구 탭(GET /api-admin/member/{id}/friends). H2 에 실제 친구 행을 만들어
 * 필터·상태별 수·페이지·순서(즐겨찾기 먼저, 표시 이름 한글 먼저)를 끝까지 확인한다.
 */
class MemberAdminFriendsTest : ApiTestSupport() {

    @Autowired lateinit var memberRepository: MemberRwRepository
    @Autowired lateinit var memberFriendRepository: MemberFriendRwRepository

    private lateinit var owner: Member
    private var seq = 0

    private fun member(username: String): Member {
        seq++
        return memberRepository.save(
            Member(
                userId = "adminfriends-$seq", email = "adminfriends-$seq@example.com", username = username,
                profiles = mutableListOf(), chatRoomMembers = mutableListOf(),
            ),
        )
    }

    /** owner 가 username 인 친구를 추가한다. alias 가 null 이면 username 이 별칭 초기값이다. */
    private fun friend(username: String, alias: String? = null, setup: MemberFriend.() -> Unit = {}): MemberFriend {
        val mf = MemberFriend.of(owner, member(username))
        if (alias != null) mf.rename(alias)
        mf.setup()
        return memberFriendRepository.save(mf)
    }

    @BeforeEach
    fun setUp() {
        owner = member("주인")
        // NORMAL 12(즐겨찾기 2), HIDDEN 2(즐겨찾기 1), BLOCKED 1 — 모두 15
        friend("가영")
        friend("Zed")
        friend("홍길동", "나비") // 별칭 기준
        friend("다희", "") // 별칭을 지우면 username 으로 선다
        friend("apple")
        for (i in 1..5) friend("사람$i")
        friend("하늘") { updateFavorite(true) }
        friend("Bob") { updateFavorite(true) }
        friend("마루") { updateFavorite(true); hide() }
        friend("숨김") { hide() }
        friend("차단") { updateFavorite(true); block() } // 차단하면 즐겨찾기가 꺼진다

        // 반대 방향(남이 owner 를 추가한 행)은 owner 의 친구가 아니다
        val stranger = member("남")
        memberFriendRepository.save(MemberFriend.of(stranger, owner))
    }

    private fun friends(vararg params: Pair<String, String>): ResultActions {
        val req = get("/api-admin/member/${owner.id}/friends").header("X-Internal-Token", "test-internal-token")
        params.forEach { (k, v) -> req.param(k, v) }
        return mockMvc.perform(req)
    }

    private fun ResultActions.names(vararg expected: String): ResultActions =
        andExpect(jsonPath("$.content[*].username", contains(*expected)))

    @Test
    fun 토큰이_없으면_403() {
        mockMvc.perform(get("/api-admin/member/${owner.id}/friends")).andExpect(status().isForbidden)
    }

    @Test
    fun 기본은_ALL_첫_페이지_10개이고_즐겨찾기_먼저_한글_먼저다() {
        friends()
            .andExpect(status().isOk)
            .names("마루", "하늘", "Bob", "가영", "홍길동", "다희", "사람1", "사람2", "사람3", "사람4")
            .andExpect(jsonPath("$.totalElements").value(15))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.content[0].favorite").value(true))
            .andExpect(jsonPath("$.content[0].friendStatus").value("HIDDEN"))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.content[4].friendName").value("나비"))
            .andExpect(jsonPath("$.content[5].friendName").value(""))
            .andExpect(jsonPath("$.content[3].favorite").value(false))
            .andExpect(jsonPath("$.content[3].friendStatus").value("NORMAL"))
            .andExpect(jsonPath("$.content[3].staffPermissions", hasSize<Any>(0)))
            .andExpect(jsonPath("$.content[3].services", hasSize<Any>(0)))
    }

    @Test
    fun 두번째_페이지는_나머지_5개다() {
        friends("page" to "1", "size" to "10")
            .andExpect(status().isOk)
            .names("사람5", "숨김", "차단", "apple", "Zed")
            .andExpect(jsonPath("$.totalElements").value(15))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.content[2].friendStatus").value("BLOCKED"))
            .andExpect(jsonPath("$.content[2].favorite").value(false))
    }

    @Test
    fun 범위를_넘은_페이지는_빈_목록이고_합계는_그대로다() {
        friends("page" to "5")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content", hasSize<Any>(0)))
            .andExpect(jsonPath("$.totalElements").value(15))
            .andExpect(jsonPath("$.number").value(5))
    }

    @Test
    fun size_는_최대_50이다() {
        friends("size" to "500")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size").value(50))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.content", hasSize<Any>(15)))
    }

    @Test
    fun 필터마다_해당_친구만_남는다() {
        friends("filter" to "NORMAL")
            .names("하늘", "Bob", "가영", "홍길동", "다희", "사람1", "사람2", "사람3", "사람4", "사람5")
            .andExpect(jsonPath("$.totalElements").value(12))
            .andExpect(jsonPath("$.totalPages").value(2))
        friends("filter" to "NORMAL", "page" to "1").names("apple", "Zed")
        friends("filter" to "FAVORITE").names("마루", "하늘", "Bob").andExpect(jsonPath("$.totalElements").value(3))
        friends("filter" to "HIDDEN").names("마루", "숨김").andExpect(jsonPath("$.totalElements").value(2))
        friends("filter" to "BLOCKED").names("차단").andExpect(jsonPath("$.totalElements").value(1))
        friends("filter" to "ALL").andExpect(jsonPath("$.totalElements").value(15))
        // 대소문자는 가리지 않는다
        friends("filter" to "favorite").andExpect(status().isOk).andExpect(jsonPath("$.totalElements").value(3))
    }

    @Test
    fun counts_는_필터와_상관없이_전체_기준이다() {
        for (filter in listOf("ALL", "NORMAL", "FAVORITE", "HIDDEN", "BLOCKED")) {
            friends("filter" to filter)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.counts.all").value(15))
                .andExpect(jsonPath("$.counts.normal").value(12))
                .andExpect(jsonPath("$.counts.favorite").value(3))
                .andExpect(jsonPath("$.counts.hidden").value(2))
                .andExpect(jsonPath("$.counts.blocked").value(1))
        }
    }

    @Test
    fun 친구가_없으면_빈_페이지와_0_counts다() {
        val lonely = member("외톨이")
        mockMvc.perform(get("/api-admin/member/${lonely.id}/friends").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content", hasSize<Any>(0)))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.counts.all").value(0))
            .andExpect(jsonPath("$.counts.favorite").value(0))
    }

    @Test
    fun 모르는_필터는_400이다() {
        friends("filter" to "DELETED").andExpect(status().isBadRequest)
    }

    @Test
    fun 없는_회원은_404다() {
        mockMvc.perform(get("/api-admin/member/987654321/friends").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun 상세의_friends_와_friendCount_는_그대로다() {
        mockMvc.perform(get("/api-admin/member/${owner.id}").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.friendCount").value(15))
            .andExpect(jsonPath("$.friends", hasSize<Any>(15)))
    }
}
