package com.example.modumessenger.data.repository

import com.example.modumessenger.core.model.FriendStatus
import com.example.modumessenger.core.model.Member
import com.example.modumessenger.core.session.BlockedUsers
import com.example.modumessenger.core.session.FriendNames
import com.example.modumessenger.core.session.SessionStore
import com.example.modumessenger.core.util.FriendFilter
import com.example.modumessenger.data.api.MemberApi
import com.example.modumessenger.data.dto.AddFriendDto
import com.example.modumessenger.data.dto.FriendFlagDto
import com.example.modumessenger.data.dto.MemberDto
import com.example.modumessenger.data.dto.PageResponseDto
import com.example.modumessenger.data.dto.RenameFriendDto
import com.example.modumessenger.data.dto.UpdateProfileDto
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 친구 즐겨찾기·숨김·차단(설계 §3). 가짜 API 로 filter 파라미터와 [BlockedUsers] 갱신을 확인한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemberRepositoryTest {

    private val me = Member(id = 1L, userId = "me", email = "me@modu.com", username = "나")

    private class FakeMemberApi : MemberApi {

        var friends: List<MemberDto> = emptyList()
        var friend: MemberDto = MemberDto(id = 2L, userId = "friend", username = "친구")
        var blockedIds: List<String> = emptyList()
        var failBlockedIds = false

        val friendsQueries = mutableListOf<FriendsQuery>()
        val favoriteCalls = mutableListOf<Pair<Long, Boolean>>()
        val hiddenCalls = mutableListOf<Pair<Long, Boolean>>()
        val blockedCalls = mutableListOf<Pair<Long, Boolean>>()
        var getFriendArg: Long? = null

        data class FriendsQuery(
            val userId: String,
            val sort: String,
            val page: Int,
            val size: Int,
            val filter: String,
        )

        override suspend fun getMemberByEmail(email: String): MemberDto = MemberDto(email = email)

        override suspend fun getMember(id: Long): MemberDto = MemberDto(id = id)

        override suspend fun updateMember(userId: String, body: UpdateProfileDto): MemberDto =
            MemberDto(userId = userId)

        override suspend fun getFriends(
            userId: String,
            sort: String,
            page: Int,
            size: Int,
            filter: String,
        ): PageResponseDto<MemberDto> {
            friendsQueries += FriendsQuery(userId, sort, page, size, filter)
            return PageResponseDto(
                content = friends,
                page = page,
                size = size,
                totalElements = friends.size.toLong(),
                totalPages = 1,
                last = true,
            )
        }

        override suspend fun getFriend(userId: String, friendMemberId: Long): MemberDto {
            getFriendArg = friendMemberId
            return friend
        }

        override suspend fun setFavorite(
            userId: String,
            friendMemberId: Long,
            body: FriendFlagDto,
        ): MemberDto {
            favoriteCalls += friendMemberId to body.on
            return friend.copy(favorite = body.on)
        }

        override suspend fun setHidden(
            userId: String,
            friendMemberId: Long,
            body: FriendFlagDto,
        ): MemberDto {
            hiddenCalls += friendMemberId to body.on
            return friend.copy(status = if (body.on) "HIDDEN" else "NORMAL")
        }

        override suspend fun setBlocked(
            userId: String,
            friendMemberId: Long,
            body: FriendFlagDto,
        ): MemberDto {
            blockedCalls += friendMemberId to body.on
            return friend.copy(
                favorite = if (body.on) false else friend.favorite,
                status = if (body.on) "BLOCKED" else "NORMAL",
            )
        }

        override suspend fun getBlockedIds(userId: String): List<String> {
            if (failBlockedIds) throw IOException("offline")
            return blockedIds
        }

        override suspend fun addFriend(userId: String, body: AddFriendDto): MemberDto =
            MemberDto(email = body.email)

        override suspend fun getFriendNames(userId: String): Map<String, String> = emptyMap()

        override suspend fun renameFriend(
            userId: String,
            friendMemberId: Long,
            body: RenameFriendDto,
        ): MemberDto = friend.copy(friendName = body.name)

        override suspend fun searchByEmail(email: String): List<MemberDto> = emptyList()

        override suspend fun deleteProfileImage(userId: String, image: String): MemberDto =
            MemberDto(userId = userId)
    }

    private fun TestScope.fixture(api: FakeMemberApi = FakeMemberApi()): Fixture {
        val sessionStore = mockk<SessionStore>(relaxed = true)
        coEvery { sessionStore.memberNow() } returns me
        coEvery { sessionStore.friendNamesJson() } returns null
        coEvery { sessionStore.blockedIdsJson() } returns null
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val blockedUsers = BlockedUsers(sessionStore, Gson(), scope)
        val repository = MemberRepositoryImpl(
            memberApi = api,
            sessionStore = sessionStore,
            friendNames = FriendNames(sessionStore, Gson(), scope),
            blockedUsers = blockedUsers,
        )
        return Fixture(api, repository, blockedUsers)
    }

    private class Fixture(
        val api: FakeMemberApi,
        val repository: MemberRepositoryImpl,
        val blockedUsers: BlockedUsers,
    )

    @Test
    fun `filter 를 주지 않으면 normal 로 묻는다`() = runTest {
        val fixture = fixture()

        fixture.repository.getFriendsPage(page = 0)
        advanceUntilIdle()

        assertEquals("normal", fixture.api.friendsQueries.single().filter)
    }

    @Test
    fun `filter 를 주면 서버 문자열 그대로 실어 보낸다`() = runTest {
        val fixture = fixture()

        fixture.repository.getFriendsPage(page = 0, size = 100, filter = FriendFilter.FAVORITE)
        fixture.repository.getFriendsPage(page = 1, filter = FriendFilter.HIDDEN)
        fixture.repository.getFriendsPage(page = 2, filter = FriendFilter.BLOCKED)
        advanceUntilIdle()

        assertEquals(
            listOf("favorite", "hidden", "blocked"),
            fixture.api.friendsQueries.map { it.filter },
        )
        assertEquals(100, fixture.api.friendsQueries.first().size)
    }

    @Test
    fun `친구 한 명을 읽으면 즐겨찾기와 상태가 모델에 실린다`() = runTest {
        val fixture = fixture()
        fixture.api.friend = MemberDto(
            id = 2L,
            userId = "friend",
            username = "친구",
            favorite = true,
            status = "HIDDEN",
        )

        val friend = fixture.repository.getFriend(2L).getOrThrow()
        advanceUntilIdle()

        assertEquals(2L, fixture.api.getFriendArg)
        assertTrue(friend.favorite)
        assertEquals(FriendStatus.HIDDEN, friend.friendStatus)
    }

    @Test
    fun `즐겨찾기와 숨김은 본문 on 으로 나간다`() = runTest {
        val fixture = fixture()

        fixture.repository.setFavorite(2L, true)
        fixture.repository.setHidden(2L, false)
        advanceUntilIdle()

        assertEquals(listOf(2L to true), fixture.api.favoriteCalls)
        assertEquals(listOf(2L to false), fixture.api.hiddenCalls)
    }

    @Test
    fun `차단하면 차단 목록에 들어가고 해제하면 빠진다`() = runTest {
        val fixture = fixture()

        fixture.repository.setBlocked(2L, true)
        advanceUntilIdle()

        assertEquals(listOf(2L to true), fixture.api.blockedCalls)
        assertEquals(setOf("friend"), fixture.blockedUsers.ids.value)
        assertTrue(fixture.blockedUsers.isBlocked("friend"))

        fixture.repository.setBlocked(2L, false)
        advanceUntilIdle()

        assertTrue(fixture.blockedUsers.ids.value.isEmpty())
        assertFalse(fixture.blockedUsers.isBlocked("friend"))
    }

    @Test
    fun `차단 목록을 받으면 빈 값은 버리고 통째로 갈아 끼운다`() = runTest {
        val fixture = fixture()
        fixture.api.blockedIds = listOf("a", "", "b", "a")

        val ids = fixture.repository.getBlockedIds().getOrThrow()
        advanceUntilIdle()

        assertEquals(setOf("a", "b"), ids)
        assertEquals(setOf("a", "b"), fixture.blockedUsers.ids.value)
    }

    @Test
    fun `차단 목록을 못 받으면 실패로 남고 이전 값을 지우지 않는다`() = runTest {
        val fixture = fixture()
        fixture.api.blockedIds = listOf("a")
        fixture.repository.getBlockedIds()
        advanceUntilIdle()

        fixture.api.failBlockedIds = true
        val result = fixture.repository.getBlockedIds()
        advanceUntilIdle()

        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(setOf("a"), fixture.blockedUsers.ids.value)
    }
}
