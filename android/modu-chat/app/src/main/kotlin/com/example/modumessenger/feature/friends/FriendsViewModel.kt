package com.example.modumessenger.feature.friends

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.modumessenger.R
import com.example.modumessenger.core.model.Member
import com.example.modumessenger.core.session.FriendNames
import com.example.modumessenger.core.session.SessionStore
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

data class FriendsUiState(
    val me: Member? = null,
    val friends: List<Member> = emptyList(),
    /** 맨 위 "즐겨찾기" 구역. 페이징하지 않고 최대 [FriendsViewModel.FAVORITES_SIZE] 명만 읽는다. */
    val favorites: List<Member> = emptyList(),
    /** 서버가 알려 준 전체 친구 수. `"친구 N 명"` 에 쓴다. */
    val totalCount: Long = 0L,
    val isLoading: Boolean = false,
) {

    /** 즐겨찾기 구역은 즐겨찾기한 친구가 있을 때만 보인다(설계 §3). */
    val showFavorites: Boolean get() = favorites.isNotEmpty()
}

/**
 * 친구 탭(부록 A §5). 페이지 크기 50, 끝에서 5개 남으면 다음 페이지를 미리 당긴다.
 * 화면이 다시 보일 때마다 [refresh] 로 목록을 통째로 다시 읽는다(기존 앱의 `onResume` 과 같다).
 */
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val sessionStore: SessionStore,
    friendNames: FriendNames,
) : ViewModel() {

    val names: StateFlow<Map<String, String>> = friendNames.names

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _errors = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** 스낵바로 띄울 문구의 리소스 id. */
    val errors: SharedFlow<Int> = _errors.asSharedFlow()

    private val pager = FriendsPager()

    private var firstResumeHandled = false

    init {
        viewModelScope.launch {
            // 서버 응답을 기다리는 동안 저장해 둔 내 정보로 카드를 먼저 채운다.
            sessionStore.memberNow()?.let { me -> _uiState.update { it.copy(me = me) } }
        }
        refresh()
    }

    /**
     * 화면이 다시 보였다. 처음 뜬 직후의 `ON_RESUME` 은 [init] 이 이미 읽었으므로 건너뛴다.
     */
    fun onResume() {
        if (!firstResumeHandled) {
            firstResumeHandled = true
            return
        }
        refresh()
    }

    /**
     * 목록을 처음부터 다시 읽는다. [FriendsPager.reset] 이 세대를 올려 늦게 온 옛 응답을 버린다.
     * 보이던 목록은 지우지 않는다 — 비웠다가 다시 채우면 화면이 한 번 깜빡인다(빈 화면 → 목록, 사진도 다시 로드).
     * 0페이지 응답이 오면 그때 통째로 바꾼다.
     */
    fun refresh() {
        pager.reset()
        _uiState.update { it.copy(isLoading = false) }
        loadMe()
        loadFavorites()
        loadNextPage()
    }

    /** 목록에서 [index] 번째가 보였다. 끝에 가까우면 다음 페이지를 당긴다. */
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
            memberRepository.getFriendsPage(page)
                .onSuccess { response ->
                    // 세대가 다르면 초기화 이후 도착한 옛 응답이므로 목록에 섞지 않는다.
                    if (pager.onLoaded(generation, response)) {
                        _uiState.update {
                            it.copy(
                                // 0페이지는 새로고침이므로 기존 목록을 통째로 바꾸고, 그 뒤 페이지는 이어 붙인다.
                                friends = if (page == 0) response.items else it.friends + response.items,
                                totalCount = pager.totalElements,
                                isLoading = false,
                            )
                        }
                    }
                }
                .onFailure {
                    pager.onFailed()
                    _uiState.update { it.copy(isLoading = false) }
                    emitError(R.string.friends_load_failed)
                }
        }
    }

    /** 즐겨찾기 구역. 실패하면 구역만 비운 채 둔다(아래 목록은 그대로 보인다). */
    private fun loadFavorites() {
        val generation = pager.generation
        viewModelScope.launch {
            memberRepository
                .getFriendsPage(page = 0, size = FAVORITES_SIZE, filter = FriendFilter.FAVORITE)
                .onSuccess { response ->
                    // 초기화 이후 도착한 옛 응답은 버린다(아래 목록과 같은 규칙).
                    if (generation != pager.generation) return@onSuccess
                    _uiState.update { it.copy(favorites = response.items) }
                }
        }
    }

    private fun loadMe() {
        viewModelScope.launch {
            memberRepository.getMe().onSuccess { me -> _uiState.update { it.copy(me = me) } }
        }
    }

    private fun emitError(@StringRes res: Int) {
        _errors.tryEmit(res)
    }

    companion object {

        /** 즐겨찾기 구역은 페이징하지 않는다. 서버가 한 번에 주는 최대치(100)를 그대로 쓴다. */
        const val FAVORITES_SIZE = 100
    }
}
