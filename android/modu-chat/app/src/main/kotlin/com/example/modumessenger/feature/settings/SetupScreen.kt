package com.example.modumessenger.feature.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import com.example.modumessenger.core.ui.theme.BrandViolet
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
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

/** 설정(부록 A §21). 네 줄 모두 실제 화면으로 간다. */
@Composable
fun SetupScreen(
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onAppInfo: () -> Unit,
    onNotice: () -> Unit,
    onSetFriends: () -> Unit,
) {
    Scaffold(
        topBar = { ModuTopBar(title = stringResource(R.string.setup_title), onBack = onBack) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SetupRow(R.drawable.ic_baseline_account_circle_24, R.string.setup_account, onAccount)
            SetupRow(R.drawable.ic_baseline_info_24, R.string.setup_app_version, onAppInfo)
            SetupRow(R.drawable.ic_baseline_celebration_24, R.string.setup_notice, onNotice)
            SetupRow(R.drawable.ic_baseline_person_24, R.string.setup_friends, onSetFriends)
        }
    }
}

@Composable
private fun SetupRow(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 설정 탭 격자·채팅방 첨부(+) 버튼과 같은 모양: 옅은 보라 원 위에 보라 아이콘.
        Box(
            modifier = Modifier
                .size(SETUP_ICON_CIRCLE)
                .background(BrandViolet.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(SETUP_ICON_SIZE),
                tint = BrandViolet,
            )
        }
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}

private val SETUP_ICON_CIRCLE = 36.dp
private val SETUP_ICON_SIZE = 20.dp
