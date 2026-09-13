package com.example.modumessenger.feature.friends

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.modumessenger.R
import com.example.modumessenger.core.model.Member
import com.example.modumessenger.core.session.FriendNames
import com.example.modumessenger.core.util.FriendFilter
import com.example.modumessenger.core.util.FriendsPager
import com.example.modumessenger.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilteredFriendsUiState(
    val friends: List<Member> = emptyList(),
    val isLoading: Boolean = false,
    /** 첫 응답이 왔다. `없습니다` 안내는 그 뒤에만 보여 준다. */
    val loaded: Boolean = false,
) {

    val showEmpty: Boolean get() = loaded && friends.isEmpty() && !isLoading
}

/**
 * 즐겨찾기·숨긴 친구·차단 친구 목록의 공통 뼈대(설계 §3). 세 화면은 [filter] 만 다르다.
 * 페이징 규칙은 친구 탭과 같다(50개씩, 끝에서 5개 남으면 미리 당긴다).
 */
abstract class FilteredFriendsViewModel(
    private val memberRepository: MemberRepository,
    friendNames: FriendNames,
    private val filter: FriendFilter,
) : ViewModel() {

    val names: StateFlow<Map<String, String>> = friendNames.names

    private val _uiState = MutableStateFlow(FilteredFriendsUiState())
    val uiState: StateFlow<FilteredFriendsUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** 스낵바로 띄울 문구의 리소스 id. */
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    private val pager = FriendsPager()

    private var firstResumeHandled = false

    init {
        refresh()
    }

    /** 화면이 다시 보였다. 처음 뜬 직후의 `ON_RESUME` 은 [init] 이 이미 읽었으므로 건너뛴다. */
    fun onResume() {
        if (!firstResumeHandled) {
            firstResumeHandled = true
            return
        }
        refresh()
    }

    fun refresh() {
        pager.reset()
        _uiState.update { it.copy(friends = emptyList(), isLoading = false, loaded = false) }
        loadNextPage()
    }

    fun onItemAppeared(index: Int) {
        val loaded = _uiState.value.friends.size
        if (index >= loaded - FriendsPager.PREFETCH_THRESHOLD) loadNextPage()
    }

    fun loadNextPage() {
        if (!pager.canLoad()) return
        val generation = pager.generation
        val page = pager.beginLoad()
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            memberRepository.getFriendsPage(page = page, filter = filter)
                .onSuccess { response ->
                    if (pager.onLoaded(generation, response)) {
                        _uiState.update {
                            it.copy(
                                friends = it.friends + response.items,
                                isLoading = false,
                                loaded = true,
                            )
                        }
                    }
                }
                .onFailure {
                    pager.onFailed()
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.tryEmit(R.string.friends_load_failed)
                }
        }
    }

    /** 행의 `해제` / `숨김 해제` / `차단 해제`. 성공하면 그 줄만 목록에서 지운다. */
    fun release(member: Member) {
        viewModelScope.launch {
            val result = when (filter) {
                FriendFilter.FAVORITE -> memberRepository.setFavorite(member.id, false)
                FriendFilter.HIDDEN -> memberRepository.setHidden(member.id, false)
                FriendFilter.BLOCKED -> memberRepository.setBlocked(member.id, false)
                FriendFilter.NORMAL -> return@launch
            }
            result
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(friends = state.friends.filterNot { it.id == member.id })
                    }
                    _messages.tryEmit(releaseMessage())
                }
                .onFailure { _messages.tryEmit(R.string.profile_flag_failed) }
        }
    }

    @StringRes
    private fun releaseMessage(): Int = when (filter) {
        FriendFilter.FAVORITE -> R.string.profile_favorite_removed
        FriendFilter.HIDDEN -> R.string.profile_hidden_undone
        FriendFilter.BLOCKED -> R.string.profile_blocked_undone
        FriendFilter.NORMAL -> R.string.profile_flag_failed
    }
}

@HiltViewModel
class FavoriteFriendsViewModel @Inject constructor(
    memberRepository: MemberRepository,
    friendNames: FriendNames,
) : FilteredFriendsViewModel(memberRepository, friendNames, FriendFilter.FAVORITE)

@HiltViewModel
class HiddenFriendsViewModel @Inject constructor(
    memberRepository: MemberRepository,
    friendNames: FriendNames,
) : FilteredFriendsViewModel(memberRepository, friendNames, FriendFilter.HIDDEN)

@HiltViewModel
class BlockedFriendsViewModel @Inject constructor(
    memberRepository: MemberRepository,
    friendNames: FriendNames,
) : FilteredFriendsViewModel(memberRepository, friendNames, FriendFilter.BLOCKED)
