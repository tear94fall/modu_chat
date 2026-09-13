package com.example.modumessenger.feature.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.modumessenger.R
import com.example.modumessenger.core.ui.components.ModuTopBar

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
            SetFriendsRow(
                R.drawable.ic_baseline_person_24,
                R.string.set_friends_favorite,
                onFavoriteFriends,
            )
            SetFriendsRow(
                R.drawable.ic_baseline_person_outline_24,
                R.string.set_friends_hidden,
                onHiddenFriends,
            )
            SetFriendsRow(
                R.drawable.ic_baseline_person_off_24,
                R.string.set_friends_blocked,
                onBlockedFriends,
            )
        }
    }
}

@Composable
private fun SetFriendsRow(iconRes: Int, labelRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}
