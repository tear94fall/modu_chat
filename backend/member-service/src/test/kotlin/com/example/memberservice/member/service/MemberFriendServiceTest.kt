package com.example.memberservice.member.service

import com.example.memberservice.global.exception.CustomException
import com.example.memberservice.member.entity.FriendStatus
import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.repository.FriendFilter
import com.example.memberservice.member.repository.FriendSort
import com.example.memberservice.member.repository.MemberFriendRepository
import com.example.memberservice.member.repository.MemberRepository
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@Transactional
class MemberFriendServiceTest {

    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var memberFriendRepository: MemberFriendRepository
    @Autowired lateinit var memberFriendService: MemberFriendService

    private fun save(username: String): Member {
        val userId = "user-" + UUID.randomUUID()
        return memberRepository.save(
            Member(userId = userId, email = "$userId@example.com", username = username, auth = "google", profiles = mutableListOf(), chatRoomMembers = mutableListOf()),
        )
    }

    @Test
    fun 친구를_추가하면_별칭은_상대_이름으로_시작하고_두_번_추가해도_행은_하나다() {
        val me = save("나")
        val friend = save("김철수")

        val first = memberFriendService.addFriend(me.userId, friend.email)
        val again = memberFriendService.addFriend(me.userId, friend.email)

        assertThat(first.friendName).isEqualTo("김철수")
        assertThat(first.username).isEqualTo("김철수")
        assertThat(again.id).isEqualTo(friend.id)
        assertThat(memberFriendRepository.countByMemberId(me.id!!)).isEqualTo(1)
        // 단방향: 상대에게는 내가 친구로 생기지 않는다
        assertThat(memberFriendRepository.countByMemberId(friend.id!!)).isEqualTo(0)
    }

    @Test
    fun 별칭을_바꾸면_목록과_이름_맵에_바뀐_이름이_나오고_상대_이름은_그대로다() {
        val me = save("나")
        val friend = save("김철수")
        memberFriendService.addFriend(me.userId, friend.email)

        val renamed = memberFriendService.renameFriend(me.userId, friend.id!!, "철수형")

        assertThat(renamed.friendName).isEqualTo("철수형")
        assertThat(renamed.username).isEqualTo("김철수")
        assertThat(memberFriendService.getFriendsPage(me.userId, FriendFilter.NORMAL, FriendSort.NAME_ASC, Pageable.unpaged()).content.map { it.friendName })
            .containsExactly("철수형")
        assertThat(memberFriendService.getFriendNames(me.userId)).isEqualTo(mapOf(friend.userId to "철수형"))
        assertThat(memberRepository.findById(friend.id!!).orElseThrow().username).isEqualTo("김철수")
    }

    @Test
    fun 친구가_아닌_사람의_별칭은_바꿀_수_없다() {
        val me = save("나")
        val stranger = save("남")

        assertThatThrownBy { memberFriendService.renameFriend(me.userId, stranger.id!!, "x") }
            .isInstanceOf(CustomException::class.java)
    }

    @Test
    fun 친구가_없으면_빈_페이지와_빈_맵이다() {
        val me = save("나")

        assertThat(memberFriendService.getFriendsPage(me.userId, FriendFilter.NORMAL, FriendSort.NAME_ASC, Pageable.unpaged()).content).isEmpty()
        assertThat(memberFriendService.getFriendNames(me.userId)).isEmpty()
        assertThat(memberFriendService.countFriends(me.id!!)).isZero()
    }

    @Test
    fun 관리자용_목록은_별칭을_함께_담는다() {
        val me = save("나")
        val friend = save("김철수")
        memberFriendService.addFriend(me.userId, friend.email)
        memberFriendService.renameFriend(me.userId, friend.id!!, "철수형")

        assertThat(memberFriendService.listForAdmin(me.id!!))
            .singleElement()
            .satisfies({ dto ->
                assertThat(dto.username).isEqualTo("김철수")
                assertThat(dto.friendName).isEqualTo("철수형")
            })
    }

    /** 새 친구는 즐겨찾기 꺼짐 + NORMAL 이다(컬럼 기본값이 H2 에서도 먹는지 함께 본다). */
    @Test
    fun 친구를_추가하면_즐겨찾기는_꺼져_있고_상태는_NORMAL_이다() {
        val me = save("나")
        val friend = save("김철수")

        val added = memberFriendService.addFriend(me.userId, friend.email)

        assertThat(added.favorite).isFalse()
        assertThat(added.status).isEqualTo(FriendStatus.NORMAL)
    }

    @Test
    fun 숨기거나_차단한_친구는_기본_목록에서_빠지고_각_필터로만_보인다() {
        val me = save("나")
        val normal = save("가")
        val hidden = save("나쁨")
        val blocked = save("다")
        memberFriendService.addFriend(me.userId, normal.email)
        memberFriendService.addFriend(me.userId, hidden.email)
        memberFriendService.addFriend(me.userId, blocked.email)
        memberFriendService.setFavorite(me.userId, normal.id!!, true)
        memberFriendService.setHidden(me.userId, hidden.id!!, true)
        memberFriendService.setBlocked(me.userId, blocked.id!!, true)

        assertThat(ids(FriendFilter.NORMAL, me)).containsExactly(normal.id)
        assertThat(ids(FriendFilter.FAVORITE, me)).containsExactly(normal.id)
        assertThat(ids(FriendFilter.HIDDEN, me)).containsExactly(hidden.id)
        assertThat(ids(FriendFilter.BLOCKED, me)).containsExactly(blocked.id)
        // 이름 맵과 친구 수는 예전대로 전부 포함한다
        assertThat(memberFriendService.getFriendNames(me.userId)).hasSize(3)
        assertThat(memberFriendService.countFriends(me.id!!)).isEqualTo(3)
        assertThat(memberFriendService.listForAdmin(me.id!!)).hasSize(3)
    }

    @Test
    fun 차단하면_즐겨찾기가_꺼지고_차단_상태에서는_즐겨찾기를_켤_수_없다() {
        val me = save("나")
        val friend = save("김철수")
        memberFriendService.addFriend(me.userId, friend.email)
        memberFriendService.setFavorite(me.userId, friend.id!!, true)

        val blocked = memberFriendService.setBlocked(me.userId, friend.id!!, true)

        assertThat(blocked.status).isEqualTo(FriendStatus.BLOCKED)
        assertThat(blocked.favorite).isFalse()
        assertThatThrownBy { memberFriendService.setFavorite(me.userId, friend.id!!, true) }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasMessageContaining("400")
    }

    @Test
    fun 숨김_해제와_차단_해제는_모두_NORMAL_로_돌아온다() {
        val me = save("나")
        val friend = save("김철수")
        memberFriendService.addFriend(me.userId, friend.email)

        memberFriendService.setHidden(me.userId, friend.id!!, true)
        assertThat(memberFriendService.setHidden(me.userId, friend.id!!, false).status).isEqualTo(FriendStatus.NORMAL)

        memberFriendService.setBlocked(me.userId, friend.id!!, true)
        assertThat(memberFriendService.setBlocked(me.userId, friend.id!!, false).status).isEqualTo(FriendStatus.NORMAL)
        // 해제해도 즐겨찾기는 돌아오지 않는다
        assertThat(memberFriendService.getFriend(me.userId, friend.id!!).favorite).isFalse()
    }

    @Test
    fun 차단_목록은_차단한_친구의_userId_만_돌려준다() {
        val me = save("나")
        val blocked = save("차단")
        val hidden = save("숨김")
        memberFriendService.addFriend(me.userId, blocked.email)
        memberFriendService.addFriend(me.userId, hidden.email)
        memberFriendService.setBlocked(me.userId, blocked.id!!, true)
        memberFriendService.setHidden(me.userId, hidden.id!!, true)

        assertThat(memberFriendService.getBlockedUserIds(me.userId)).containsExactly(blocked.userId)
    }

    @Test
    fun 친구가_아닌_사람의_상태는_읽지도_바꾸지도_못한다() {
        val me = save("나")
        val other = save("남")
        val friendOfOther = save("남의친구")
        memberFriendService.addFriend(other.userId, friendOfOther.email)

        assertThatThrownBy { memberFriendService.getFriend(me.userId, friendOfOther.id!!) }.isInstanceOf(CustomException::class.java)
        assertThatThrownBy { memberFriendService.setFavorite(me.userId, friendOfOther.id!!, true) }.isInstanceOf(CustomException::class.java)
        assertThatThrownBy { memberFriendService.setHidden(me.userId, friendOfOther.id!!, true) }.isInstanceOf(CustomException::class.java)
        assertThatThrownBy { memberFriendService.setBlocked(me.userId, friendOfOther.id!!, true) }.isInstanceOf(CustomException::class.java)
    }

    private fun ids(filter: FriendFilter, me: Member): List<Long?> =
        memberFriendService.getFriendsPage(me.userId, filter, FriendSort.NAME_ASC, Pageable.unpaged()).content.map { it.id }
}
