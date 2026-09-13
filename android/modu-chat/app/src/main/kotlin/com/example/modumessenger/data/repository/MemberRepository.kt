package com.example.modumessenger.data.repository

import com.example.modumessenger.core.model.Member
import com.example.modumessenger.core.network.safeCall
import com.example.modumessenger.core.session.BlockedUsers
import com.example.modumessenger.core.session.FriendNames
import com.example.modumessenger.core.session.SessionStore
import com.example.modumessenger.core.util.FriendFilter
import com.example.modumessenger.core.util.FriendSort
import com.example.modumessenger.core.util.FriendsPager
import com.example.modumessenger.data.api.MemberApi
import com.example.modumessenger.data.dto.AddFriendDto
import com.example.modumessenger.data.dto.FriendFlagDto
import com.example.modumessenger.data.dto.PageResponseDto
import com.example.modumessenger.data.dto.RenameFriendDto
import com.example.modumessenger.data.dto.UpdateProfileDto
import com.example.modumessenger.data.dto.mapContent
import com.example.modumessenger.data.dto.toModel
import javax.inject.Inject
import javax.inject.Singleton

interface MemberRepository {

    /** 세션에 저장된 내 이메일로 서버의 최신 내 정보를 받아 세션을 갱신한다. */
    suspend fun getMe(): Result<Member>

    suspend fun getMember(id: Long): Result<Member>

    /** 내 프로필 수정. 성공하면 세션의 `member` 도 갱신한다. */
    suspend fun updateProfile(dto: UpdateProfileDto): Result<Member>

    /** [filter] 는 서버가 아는 `normal|favorite|hidden|blocked` 중 하나다. */
    suspend fun getFriendsPage(
        page: Int,
        size: Int = FriendsPager.DEFAULT_PAGE_SIZE,
        sort: String = FriendSort.DEFAULT,
        filter: FriendFilter = FriendFilter.NORMAL,
    ): Result<PageResponseDto<Member>>

    /** 친구 한 명의 상태(즐겨찾기·숨김·차단). 친구가 아니면 실패다. */
    suspend fun getFriend(friendMemberId: Long): Result<Member>

    /** 차단한 친구에게는 걸 수 없다(서버가 400 을 준다). */
    suspend fun setFavorite(friendMemberId: Long, on: Boolean): Result<Member>

    suspend fun setHidden(friendMemberId: Long, on: Boolean): Result<Member>

    /** 성공하면 [BlockedUsers] 도 함께 갱신한다(말풍선·배너·알림이 바로 따라간다). */
    suspend fun setBlocked(friendMemberId: Long, on: Boolean): Result<Member>

    /** 차단 목록을 서버에서 받아 [BlockedUsers] 를 통째로 갈아 끼운다. */
    suspend fun getBlockedIds(): Result<Set<String>>

    suspend fun addFriend(email: String): Result<Member>

    /** 서버의 별칭 맵을 받아 [FriendNames] 를 통째로 갈아 끼운다. */
    suspend fun loadFriendNames(): Result<Map<String, String>>

    suspend fun renameFriend(friendMemberId: Long, name: String): Result<Member>

    suspend fun searchByEmail(email: String): Result<List<Member>>

    suspend fun deleteProfileImage(image: String): Result<Member>
}

@Singleton
class MemberRepositoryImpl @Inject constructor(
    private val memberApi: MemberApi,
    private val sessionStore: SessionStore,
    private val friendNames: FriendNames,
    private val blockedUsers: BlockedUsers,
) : MemberRepository {

    override suspend fun getMe(): Result<Member> = safeCall {
        val email = requireMe().email
        val member = memberApi.getMemberByEmail(email).toModel()
        sessionStore.saveMember(member)
        member
    }

    override suspend fun getMember(id: Long): Result<Member> = safeCall {
        memberApi.getMember(id).toModel()
    }

    override suspend fun updateProfile(dto: UpdateProfileDto): Result<Member> = safeCall {
        val member = memberApi.updateMember(requireMe().userId, dto).toModel()
        sessionStore.saveMember(member)
        member
    }

    override suspend fun getFriendsPage(
        page: Int,
        size: Int,
        sort: String,
        filter: FriendFilter,
    ): Result<PageResponseDto<Member>> = safeCall {
        val response = memberApi.getFriends(requireMe().userId, sort, page, size, filter.value)
        // 목록이 들고 오는 별칭을 캐시에 반영해 둔다(알림·채팅방 이름이 같은 이름을 쓰도록).
        response.items.forEach { dto ->
            val userId = dto.userId
            if (!userId.isNullOrBlank()) friendNames.put(userId, dto.friendName)
        }
        response.mapContent { it.toModel() }
    }

    override suspend fun getFriend(friendMemberId: Long): Result<Member> = safeCall {
        memberApi.getFriend(requireMe().userId, friendMemberId).toModel()
    }

    override suspend fun setFavorite(friendMemberId: Long, on: Boolean): Result<Member> = safeCall {
        memberApi.setFavorite(requireMe().userId, friendMemberId, FriendFlagDto(on)).toModel()
    }

    override suspend fun setHidden(friendMemberId: Long, on: Boolean): Result<Member> = safeCall {
        memberApi.setHidden(requireMe().userId, friendMemberId, FriendFlagDto(on)).toModel()
    }

    override suspend fun setBlocked(friendMemberId: Long, on: Boolean): Result<Member> = safeCall {
        val friend = memberApi
            .setBlocked(requireMe().userId, friendMemberId, FriendFlagDto(on))
            .toModel()
        blockedUsers.set(friend.userId, on)
        friend
    }

    override suspend fun getBlockedIds(): Result<Set<String>> = safeCall {
        val ids = memberApi.getBlockedIds(requireMe().userId)
            .filter { it.isNotBlank() }
            .toSet()
        blockedUsers.replaceAll(ids)
        ids
    }

    override suspend fun addFriend(email: String): Result<Member> = safeCall {
        val friend = memberApi.addFriend(requireMe().userId, AddFriendDto(email)).toModel()
        if (friend.userId.isNotBlank()) friendNames.put(friend.userId, friend.friendName)
        friend
    }

    override suspend fun loadFriendNames(): Result<Map<String, String>> = safeCall {
        val names = memberApi.getFriendNames(requireMe().userId)
        friendNames.replaceAll(names)
        names
    }

    override suspend fun renameFriend(friendMemberId: Long, name: String): Result<Member> = safeCall {
        val friend = memberApi
            .renameFriend(requireMe().userId, friendMemberId, RenameFriendDto(name))
            .toModel()
        if (friend.userId.isNotBlank()) friendNames.put(friend.userId, name)
        friend
    }

    override suspend fun searchByEmail(email: String): Result<List<Member>> = safeCall {
        memberApi.searchByEmail(email).map { it.toModel() }
    }

    override suspend fun deleteProfileImage(image: String): Result<Member> = safeCall {
        val member = memberApi.deleteProfileImage(requireMe().userId, image).toModel()
        sessionStore.saveMember(member)
        member
    }

    private suspend fun requireMe(): Member =
        sessionStore.memberNow() ?: throw IllegalStateException("로그인 정보가 없다")
}
