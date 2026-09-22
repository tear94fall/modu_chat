package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.entity.Role
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

/**
 * 백오피스 회원 목록 정렬을 H2 에서 확인한다. 이름 규칙은 앱 친구 목록([FriendSort])과 같아야 한다:
 * 한글 이름이 먼저, 그 다음 영문·숫자·기호, 이름이 빈 회원은 항상 마지막.
 * 다른 테스트가 남긴 회원과 섞이지 않게 이메일에 심어 둔 표식으로 좁혀서 본다.
 */
@SpringBootTest
@Transactional
class MemberAdminSortTest {

    companion object {
        private const val MARK = "sortcase"
        private fun email(userId: String) = "$MARK-$userId@example.com"
    }

    @Autowired lateinit var memberRepository: MemberRepository

    private fun save(userId: String, username: String?, role: Role? = null): Member = memberRepository.save(
        Member(userId = "$MARK-$userId", email = email(userId), username = username, role = role, profiles = mutableListOf(), chatRoomMembers = mutableListOf()),
    )

    private fun saveAll() {
        save("1", "김철수")
        save("2", "Alice")
        save("3", "박영희")
        save("4", "bob")
        save("5", "3반 민수")
    }

    private fun usernames(keyword: String, sort: MemberSort) =
        memberRepository.searchForAdmin(keyword, sort, Pageable.unpaged()).content.map { it.username }

    @Test
    fun 기본은_이름_오름차순이고_한글_이름이_먼저다() {
        saveAll()
        assertThat(usernames(MARK, MemberSort.NAME_ASC)).containsExactly("김철수", "박영희", "3반 민수", "Alice", "bob")
    }

    /** 숫자로 시작하는 이름은 한글이 아니므로 영문과 같은 그룹이고, 그 안에서는 코드포인트 순이다. */
    @Test
    fun 내림차순은_그룹_순서만_뒤집고_이름은_역순이다() {
        saveAll()
        assertThat(usernames(MARK, MemberSort.NAME_DESC)).containsExactly("bob", "Alice", "3반 민수", "박영희", "김철수")
    }

    @Test
    fun 이름이_없거나_빈_회원은_방향과_상관없이_마지막이다() {
        saveAll()
        save("6", null)
        save("7", "")

        for (sort in listOf(MemberSort.NAME_ASC, MemberSort.NAME_DESC)) {
            val userIds = memberRepository.searchForAdmin(MARK, sort, Pageable.unpaged()).content.map { it.userId }
            // 이름이 빈 둘끼리의 순서는 DB 의 NULL 취급에 달려 있어 정하지 않는다. 둘 다 뒤로 밀렸는지만 본다.
            assertThat(userIds.subList(userIds.size - 2, userIds.size)).containsExactlyInAnyOrder("$MARK-6", "$MARK-7")
        }
    }

    @Test
    fun 가입일_정렬도_고를_수_있다() {
        saveAll()
        assertThat(usernames(MARK, MemberSort.CREATED_DESC)).containsExactly("3반 민수", "bob", "박영희", "Alice", "김철수")
        assertThat(usernames(MARK, MemberSort.CREATED_ASC)).containsExactly("김철수", "Alice", "박영희", "bob", "3반 민수")
    }

    @Test
    fun 검색어는_이메일이나_이름에_대소문자_구분_없이_걸린다() {
        saveAll()
        assertThat(usernames("ALICE", MemberSort.NAME_ASC)).containsExactly("Alice")
        assertThat(usernames("$MARK-4", MemberSort.NAME_ASC)).containsExactly("bob")
    }

    @Test
    fun 페이지를_잘라도_전체_건수가_맞는다() {
        saveAll()

        val first = memberRepository.searchForAdmin(MARK, MemberSort.NAME_ASC, PageRequest.of(0, 2))
        val second = memberRepository.searchForAdmin(MARK, MemberSort.NAME_ASC, PageRequest.of(1, 2))

        assertThat(first.content.map { it.username }).containsExactly("김철수", "박영희")
        assertThat(first.totalElements).isEqualTo(5)
        assertThat(first.totalPages).isEqualTo(3)
        assertThat(second.content.map { it.username }).containsExactly("3반 민수", "Alice")
    }

    /** 이메일과 사용자 ID 는 심어 둔 표식 덕분에 1~5 순서 그대로 줄선다. */
    @Test
    fun 이메일_정렬도_고를_수_있다() {
        saveAll()
        assertThat(memberRepository.searchForAdmin(MARK, MemberSort.EMAIL_ASC, Pageable.unpaged()).content.map { it.email })
            .containsExactly(email("1"), email("2"), email("3"), email("4"), email("5"))
        assertThat(memberRepository.searchForAdmin(MARK, MemberSort.EMAIL_DESC, Pageable.unpaged()).content.map { it.email })
            .containsExactly(email("5"), email("4"), email("3"), email("2"), email("1"))
    }

    @Test
    fun 사용자_ID_정렬도_고를_수_있다() {
        saveAll()
        assertThat(memberRepository.searchForAdmin(MARK, MemberSort.USER_ID_ASC, Pageable.unpaged()).content.map { it.userId })
            .containsExactly("$MARK-1", "$MARK-2", "$MARK-3", "$MARK-4", "$MARK-5")
        assertThat(memberRepository.searchForAdmin(MARK, MemberSort.USER_ID_DESC, Pageable.unpaged()).content.map { it.userId })
            .containsExactly("$MARK-5", "$MARK-4", "$MARK-3", "$MARK-2", "$MARK-1")
    }

    /** 권한은 저장된 이름 순이라 ROLE_ADMIN 이 ROLE_MEMBER 보다 앞이다. 같은 권한끼리는 id 로 고정된다. */
    @Test
    fun 권한_정렬도_고를_수_있다() {
        save("1", "김철수", Role.ROLE_MEMBER)
        save("2", "Alice", Role.ROLE_ADMIN)
        save("3", "박영희", Role.ROLE_MEMBER)

        assertThat(usernames(MARK, MemberSort.ROLE_ASC)).containsExactly("Alice", "김철수", "박영희")
        assertThat(usernames(MARK, MemberSort.ROLE_DESC)).containsExactly("박영희", "김철수", "Alice")
    }

    @Test
    fun 허용_목록에_없는_정렬_문자열은_받지_않는다() {
        assertThat(MemberSort.parse("name")).isEqualTo(MemberSort.NAME_ASC)
        assertThat(MemberSort.parse("name,desc")).isEqualTo(MemberSort.NAME_DESC)
        assertThat(MemberSort.parse("email,asc")).isEqualTo(MemberSort.EMAIL_ASC)
        assertThat(MemberSort.parse("email,desc")).isEqualTo(MemberSort.EMAIL_DESC)
        assertThat(MemberSort.parse("userId,asc")).isEqualTo(MemberSort.USER_ID_ASC)
        assertThat(MemberSort.parse("userId,desc")).isEqualTo(MemberSort.USER_ID_DESC)
        assertThat(MemberSort.parse("role,asc")).isEqualTo(MemberSort.ROLE_ASC)
        assertThat(MemberSort.parse("role,desc")).isEqualTo(MemberSort.ROLE_DESC)
        assertThat(MemberSort.parse("createdDate,desc")).isEqualTo(MemberSort.CREATED_DESC)
        assertThat(MemberSort.parse("createdDate,asc")).isEqualTo(MemberSort.CREATED_ASC)
        // 목록에 값이 보이지 않는 열은 여전히 못 고른다.
        assertThat(MemberSort.parse("statusMessage,asc")).isNull()
        assertThat(MemberSort.parse("name,sideways")).isNull()
        assertThat(MemberSort.parse("")).isNull()
        assertThat(MemberSort.parse(null)).isNull()
    }
}
