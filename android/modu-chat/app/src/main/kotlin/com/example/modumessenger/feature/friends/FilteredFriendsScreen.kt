package com.example.modumessenger.feature.friends

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.modumessenger.R
import com.example.modumessenger.core.ui.components.ModuTopBar

/** 친구 설정 → 즐겨찾기(설계 §3). 행마다 `해제`. */
@Composable
fun FavoriteFriendsScreen(
    onBack: () -> Unit,
    onOpenProfile: (Long) -> Unit,
    viewModel: FavoriteFriendsViewModel = hiltViewModel(),
) {
    FilteredFriendsScreen(
        titleRes = R.string.favorite_friends_title,
        emptyRes = R.string.favorite_friends_empty,
        actionRes = R.string.favorite_friends_action,
        onBack = onBack,
        onOpenProfile = onOpenProfile,
        viewModel = viewModel,
    )
}

/** 친구 설정 → 숨긴 친구. 행마다 `숨김 해제`. */
@Composable
fun HiddenFriendsScreen(
    onBack: () -> Unit,
    onOpenProfile: (Long) -> Unit,
    viewModel: HiddenFriendsViewModel = hiltViewModel(),
) {
    FilteredFriendsScreen(
        titleRes = R.string.hidden_friends_title,
        emptyRes = R.string.hidden_friends_empty,
        actionRes = R.string.hidden_friends_action,
        onBack = onBack,
        onOpenProfile = onOpenProfile,
        viewModel = viewModel,
    )
}

/** 친구 설정 → 차단 친구. 행마다 `차단 해제`. */
@Composable
fun BlockedFriendsScreen(
    onBack: () -> Unit,
    onOpenProfile: (Long) -> Unit,
    viewModel: BlockedFriendsViewModel = hiltViewModel(),
) {
    FilteredFriendsScreen(
        titleRes = R.string.blocked_friends_title,
        emptyRes = R.string.blocked_friends_empty,
        actionRes = R.string.blocked_friends_action,
        onBack = onBack,
        onOpenProfile = onOpenProfile,
        viewModel = viewModel,
    )
}

/** 세 화면의 몸통. 목록 + 행마다 해제 버튼 + 비었을 때 안내. */
@Composable
private fun FilteredFriendsScreen(
    @StringRes titleRes: Int,
    @StringRes emptyRes: Int,
    @StringRes actionRes: Int,
    onBack: () -> Unit,
    onOpenProfile: (Long) -> Unit,
    viewModel: FilteredFriendsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val names by viewModel.names.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 프로필에서 상태를 바꾸고 돌아오면 목록이 달라져 있다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { res ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(res))
        }
    }

    Scaffold(
        topBar = {
            ModuTopBar(title = stringResource(titleRes), onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.showEmpty) {
                Text(
                    text = stringResource(emptyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(uiState.friends, key = { _, member -> member.id }) { index, member ->
                        LaunchedEffect(index, uiState.friends.size) { viewModel.onItemAppeared(index) }
                        FriendRow(
                            member = member,
                            names = names,
                            onClick = { onOpenProfile(member.id) },
                        ) {
                            TextButton(onClick = { viewModel.release(member) }) {
                                Text(stringResource(actionRes))
                            }
                        }
                    }
                }
            }
        }
    }
}
