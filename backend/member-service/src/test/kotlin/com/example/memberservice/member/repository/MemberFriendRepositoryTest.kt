package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.entity.MemberFriend
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

/** member_friend 가 별칭(friend_name) 기준으로 정렬·페이징되는지 H2 에서 검증한다. */
@SpringBootTest
@Transactional
class MemberFriendRepositoryTest {

    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var memberFriendRepository: MemberFriendRepository

    private fun save(userId: String, email: String, username: String?): Member = memberRepository.save(
        Member(userId = userId, email = email, username = username, profiles = mutableListOf(), chatRoomMembers = mutableListOf()),
    )

    /** me 가 friend 를 추가한다. alias 가 null 이면 상대 username 이 초기값이다. */
    private fun befriend(me: Member, friend: Member, alias: String?): MemberFriend {
        val mf = MemberFriend.of(me, friend)
        if (alias != null) mf.rename(alias)
        return memberFriendRepository.save(mf)
    }

    @Test
    fun 추가하면_별칭은_상대의_현재_이름이고_이름이_없으면_빈_문자열이다() {
        val me = save("me", "me@example.com", "나")
        val named = save("f1", "f1@example.com", "김철수")
        val unnamed = save("f2", "f2@example.com", null)

        assertThat(befriend(me, named, null).friendName).isEqualTo("김철수")
        assertThat(befriend(me, unnamed, null).friendName).isEqualTo("")
    }

    @Test
    fun 정렬은_상대_이름이_아니라_내가_정한_별칭_기준이다() {
        val me = save("me", "me@example.com", "나")
        val a = save("a", "a@example.com", "홍길동") // 별칭 "가나"
        val b = save("b", "b@example.com", "강감찬") // 별칭 "Zoe"
        val c = save("c", "c@example.com", "김철수") // 별칭 없음 → "김철수"
        val d = save("d", "d@example.com", "박영희") // 별칭 "" (지움)
        befriend(me, a, "가나")
        befriend(me, b, "Zoe")
        befriend(me, c, null)
        befriend(me, d, "")

        val page = memberFriendRepository.findPage(me.id!!, FriendSort.NAME_ASC, Pageable.unpaged())

        assertThat(page.content.map { it.friend.email })
            .containsExactly("a@example.com", "c@example.com", "b@example.com", "d@example.com")
        assertThat(page.content[0].friend.username).isEqualTo("홍길동")
    }

    @Test
    fun 내림차순과_이메일순도_별칭_기준으로_동작한다() {
        val me = save("me", "me@example.com", "나")
        befriend(me, save("a", "c@example.com", "x"), "가")
        befriend(me, save("b", "a@example.com", "y"), "나")
        befriend(me, save("c", "b@example.com", "z"), "Alice")

        assertThat(memberFriendRepository.findPage(me.id!!, FriendSort.NAME_DESC, Pageable.unpaged()).content.map { it.friendName })
            .containsExactly("Alice", "나", "가")
        assertThat(memberFriendRepository.findPage(me.id!!, FriendSort.EMAIL_ASC, Pageable.unpaged()).content.map { it.friend.email })
            .containsExactly("a@example.com", "b@example.com", "c@example.com")
    }

    @Test
    fun 페이징과_카운트와_단건_조회가_된다() {
        val me = save("me", "me@example.com", "나")
        val other = save("other", "other@example.com", "남")
        val f1 = save("f1", "f1@example.com", "다")
        befriend(me, f1, null)
        befriend(me, save("f2", "f2@example.com", "가"), null)
        befriend(me, save("f3", "f3@example.com", "나"), null)
        befriend(other, f1, null) // 다른 사람의 친구는 섞이면 안 된다

        val first = memberFriendRepository.findPage(me.id!!, FriendSort.NAME_ASC, PageRequest.of(0, 2))
        val second = memberFriendRepository.findPage(me.id!!, FriendSort.NAME_ASC, PageRequest.of(1, 2))

        assertThat(first.content.map { it.friendName }).containsExactly("가", "나")
        assertThat(first.totalElements).isEqualTo(3)
        assertThat(first.isLast).isFalse()
        assertThat(second.content.map { it.friendName }).containsExactly("다")
        assertThat(memberFriendRepository.countByMemberId(me.id!!)).isEqualTo(3)
        assertThat(memberFriendRepository.findByMemberIdAndFriendId(me.id!!, f1.id!!)).isPresent
        assertThat(memberFriendRepository.findByMemberIdAndFriendId(other.id!!, me.id!!)).isEmpty
        assertThat(memberFriendRepository.findAllByMemberIdWithFriend(me.id!!)).hasSize(3)
    }

    @Test
    fun 같은_친구를_두_번_추가하면_유니크_제약에_걸린다() {
        val me = save("me", "me@example.com", "나")
        val f = save("f", "f@example.com", "친구")
        befriend(me, f, null)

        assertThrows(DataIntegrityViolationException::class.java) {
            befriend(me, f, null)
            memberFriendRepository.flush()
        }
    }
}
