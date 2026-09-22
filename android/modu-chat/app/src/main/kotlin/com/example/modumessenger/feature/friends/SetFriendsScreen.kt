package com.example.modumessenger.feature.friends

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.modumessenger.R
import com.example.modumessenger.core.ui.components.ModuTopBar
import com.example.modumessenger.core.ui.components.SettingsRow

/** 친구 설정(부록 A §20). 세 항목이 각각 즐겨찾기·숨긴 친구·차단 친구 목록으로 간다. */
@Composable
fun SetFriendsScreen(
    onBack: () -> Unit,
    onFavoriteFriends: () -> Unit,
    onHiddenFriends: () -> Unit,
    onBlockedFriends: () -> Unit,
) {
    Scaffold(
        topBar = { ModuTopBar(title = stringResource(R.string.set_friends_title), onBack = onBack) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SettingsRow(stringResource(R.string.set_friends_favorite), onClick = onFavoriteFriends, icon = painterResource(R.drawable.ic_baseline_person_24))
            SettingsRow(stringResource(R.string.set_friends_hidden), onClick = onHiddenFriends, icon = painterResource(R.drawable.ic_baseline_person_outline_24))
            SettingsRow(stringResource(R.string.set_friends_blocked), onClick = onBlockedFriends, icon = painterResource(R.drawable.ic_baseline_person_off_24))
        }
    }
}
