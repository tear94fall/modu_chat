package com.example.modumessenger.feature.profile

import androidx.lifecycle.SavedStateHandle
import com.example.modumessenger.R
import com.example.modumessenger.core.model.FriendStatus
import com.example.modumessenger.core.model.Member
import com.example.modumessenger.core.model.Profile
import com.example.modumessenger.core.model.ProfileType
import com.example.modumessenger.core.session.FriendNames
import com.example.modumessenger.core.session.SessionStore
import com.example.modumessenger.navigation.Routes
import com.example.modumessenger.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val me = Member(id = 1L, userId = "me", username = "나")

    private fun viewModel(
        memberId: Long,
        repository: FakeMemberRepository,
        roomCreator: FakeRoomCreator = FakeRoomCreator(),
    ): ProfileViewModel {
        val sessionStore = mockk<SessionStore>()
        coEvery { sessionStore.memberNow() } returns me
        val friendNames = mockk<FriendNames>()
        every { friendNames.names } returns MutableStateFlow(emptyMap())
        return ProfileViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_MEMBER_ID to memberId)),
            memberRepository = repository,
            sessionStore = sessionStore,
            friendNames = friendNames,
            roomCreator = roomCreator,
        )
    }

    @Test
    fun `내 프로필이면 편집 버튼이 보이고 이름 변경은 숨긴다`() = runTest {
        val repository = FakeMemberRepository(member = me, me = me)
        val vm = viewModel(memberId = me.id, repository = repository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isMe)
        assertTrue(state.showEditButton)
        assertFalse(state.showRenameButton)
    }

    @Test
    fun `남의 프로필이면 이름 변경이 보이고 편집은 숨긴다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val repository = FakeMemberRepository(member = friend, me = me)
        val vm = viewModel(memberId = friend.id, repository = repository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isMe)
        assertFalse(state.showEditButton)
        assertTrue(state.showRenameButton)
    }

    @Test
    fun `기록이 없으면 기록 버튼을 숨기고 있으면 보여 준다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val empty = FakeMemberRepository(member = friend, me = me)
        val emptyVm = viewModel(memberId = friend.id, repository = empty)
        advanceUntilIdle()
        assertFalse(emptyVm.uiState.value.showHistoryButton)

        val withHistory = FakeMemberRepository(
            member = friend.copy(
                profiles = listOf(
                    Profile(id = 9L, memberId = 2L, type = ProfileType.PROFILE_IMAGE, value = "a.jpg"),
                ),
            ),
            me = me,
        )
        val historyVm = viewModel(memberId = friend.id, repository = withHistory)
        advanceUntilIdle()
        assertTrue(historyVm.uiState.value.showHistoryButton)
    }

    @Test
    fun `이름이 비어 있으면 저장할 수 없다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val repository = FakeMemberRepository(member = friend, me = me)
        val vm = viewModel(memberId = friend.id, repository = repository)
        advanceUntilIdle()

        vm.showRenameDialog()
        assertEquals("친구", vm.uiState.value.renameInput)
        assertTrue(vm.uiState.value.canSaveRename)

        vm.onRenameInputChange("   ")
        assertFalse(vm.uiState.value.canSaveRename)

        vm.saveRename()
        advanceUntilIdle()
        assertTrue(repository.renamedTo.isEmpty())

        vm.onRenameInputChange(" 단짝 ")
        assertTrue(vm.uiState.value.canSaveRename)
        vm.saveRename()
        advanceUntilIdle()

        // 앞뒤 공백은 떼고 보낸다.
        assertEquals(listOf(2L to "단짝"), repository.renamedTo)
        assertFalse(vm.uiState.value.renameDialogVisible)
    }

    @Test
    fun `나와 채팅 하기는 내 id 하나만 보낸다`() = runTest {
        val repository = FakeMemberRepository(member = me, me = me)
        val roomCreator = FakeRoomCreator()
        val vm = viewModel(memberId = me.id, repository = repository, roomCreator = roomCreator)
        advanceUntilIdle()

        vm.startChat()
        advanceUntilIdle()

        assertEquals(listOf(listOf(1L)), roomCreator.requested)
    }

    @Test
    fun `친구와 채팅 하기는 내 id 와 상대 id 를 보낸다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val repository = FakeMemberRepository(member = friend, me = me)
        val roomCreator = FakeRoomCreator()
        val vm = viewModel(memberId = friend.id, repository = repository, roomCreator = roomCreator)
        advanceUntilIdle()

        vm.startChat()
        advanceUntilIdle()

        assertEquals(listOf(listOf(1L, 2L)), roomCreator.requested)
    }

    // ---------- 즐겨찾기·숨김·차단(설계 §3) ----------

    /** 스낵바로 흘린 문구를 모은다. `messages` 는 replay 가 없어 곧바로 붙어 있어야 한다. */
    private fun TestScope.collectMessages(vm: ProfileViewModel): MutableList<ProfileMessage> {
        val collected = mutableListOf<ProfileMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.messages.collect { collected += it }
        }
        return collected
    }

    @Test
    fun `친구면 별과 메뉴가 보이고 친구가 아니면 숨긴다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val repository = FakeMemberRepository(member = friend, me = me)
        repository.friend = friend.copy(favorite = true)
        val vm = viewModel(memberId = friend.id, repository = repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isFriend)
        assertTrue(vm.uiState.value.showFriendActions)
        assertTrue(vm.uiState.value.favorite)

        val stranger = FakeMemberRepository(member = friend, me = me)
        stranger.friend = null
        val strangerVm = viewModel(memberId = friend.id, repository = stranger)
        advanceUntilIdle()

        assertFalse(strangerVm.uiState.value.isFriend)
        assertFalse(strangerVm.uiState.value.showFriendActions)
    }

    @Test
    fun `내 프로필에서는 별도 메뉴도 없다`() = runTest {
        val repository = FakeMemberRepository(member = me, me = me)
        val vm = viewModel(memberId = me.id, repository = repository)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showFriendActions)
        assertFalse(vm.uiState.value.canToggleFavorite)
        // 내 프로필에서는 친구 상태를 묻지도 않는다.
        assertTrue(repository.favoriteCalls.isEmpty())
    }

    @Test
    fun `별을 누르면 즐겨찾기를 걸고 다시 누르면 뺀다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val repository = FakeMemberRepository(member = friend, me = me)
        val vm = viewModel(memberId = friend.id, repository = repository)
        advanceUntilIdle()
        val messages = collectMessages(vm)

        vm.toggleFavorite()
        advanceUntilIdle()

        assertEquals(listOf(2L to true), repository.favoriteCalls)
        assertTrue(vm.uiState.value.favorite)
        assertEquals(R.string.profile_favorite_added, messages.last().res)

        vm.toggleFavorite()
        advanceUntilIdle()

        assertEquals(listOf(2L to true, 2L to false), repository.favoriteCalls)
        assertFalse(vm.uiState.value.favorite)
        assertEquals(R.string.profile_favorite_removed, messages.last().res)
    }

    @Test
    fun `차단한 친구는 별 토글이 죽는다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val repository = FakeMemberRepository(member = friend, me = me)
        repository.friend = friend.copy(friendStatus = FriendStatus.BLOCKED)
        val vm = viewModel(memberId = friend.id, repository = repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isBlocked)
        assertFalse(vm.uiState.value.canToggleFavorite)

        vm.toggleFavorite()
        advanceUntilIdle()

        assertTrue(repository.favoriteCalls.isEmpty())
    }

    @Test
    fun `숨기기는 확인 팝업을 거쳐 숨김으로 바뀌고 해제는 바로 푼다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val repository = FakeMemberRepository(member = friend, me = me)
        val vm = viewModel(memberId = friend.id, repository = repository)
        advanceUntilIdle()
        val messages = collectMessages(vm)

        vm.showHideDialog()
        assertTrue(vm.uiState.value.hideDialogVisible)

        vm.setHidden(true)
        advanceUntilIdle()

        assertEquals(listOf(2L to true), repository.hiddenCalls)
        assertFalse(vm.uiState.value.hideDialogVisible)
        assertTrue(vm.uiState.value.isHidden)
        assertEquals(R.string.profile_hidden_done, messages.last().res)

        vm.setHidden(false)
        advanceUntilIdle()

        assertEquals(listOf(2L to true, 2L to false), repository.hiddenCalls)
        assertFalse(vm.uiState.value.isHidden)
        assertEquals(R.string.profile_hidden_undone, messages.last().res)
    }

    @Test
    fun `차단은 확인 팝업을 거쳐 차단으로 바뀌고 즐겨찾기는 풀린다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val repository = FakeMemberRepository(member = friend, me = me)
        repository.friend = friend.copy(favorite = true)
        val vm = viewModel(memberId = friend.id, repository = repository)
        advanceUntilIdle()
        val messages = collectMessages(vm)

        vm.showBlockDialog()
        assertTrue(vm.uiState.value.blockDialogVisible)

        vm.setBlocked(true)
        advanceUntilIdle()

        assertEquals(listOf(2L to true), repository.blockedCalls)
        assertFalse(vm.uiState.value.blockDialogVisible)
        assertTrue(vm.uiState.value.isBlocked)
        assertFalse(vm.uiState.value.favorite)
        assertEquals(R.string.profile_blocked_done, messages.last().res)

        vm.setBlocked(false)
        advanceUntilIdle()

        assertEquals(listOf(2L to true, 2L to false), repository.blockedCalls)
        assertFalse(vm.uiState.value.isBlocked)
        assertEquals(R.string.profile_blocked_undone, messages.last().res)
    }

    @Test
    fun `바꾸지 못하면 상태는 그대로 두고 실패 문구만 띄운다`() = runTest {
        val friend = Member(id = 2L, userId = "friend", username = "친구")
        val repository = FakeMemberRepository(member = friend, me = me)
        val vm = viewModel(memberId = friend.id, repository = repository)
        advanceUntilIdle()
        val messages = collectMessages(vm)

        repository.flagResult = Result.failure(IOException("offline"))
        vm.setBlocked(true)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isBlocked)
        assertFalse(vm.uiState.value.updatingFlag)
        assertEquals(R.string.profile_flag_failed, messages.last().res)
    }
}
