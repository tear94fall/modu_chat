package com.example.modumessenger.feature.settings

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

/** 설정(부록 A §21). 다섯 줄 모두 실제 화면으로 간다. 줄 모양은 하위 화면들과 같은 [SettingsRow] 다. */
@Composable
fun SetupScreen(
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onAppInfo: () -> Unit,
    onNotice: () -> Unit,
    onSetFriends: () -> Unit,
    onLockSettings: () -> Unit,
) {
    Scaffold(
        topBar = { ModuTopBar(title = stringResource(R.string.setup_title), onBack = onBack) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SettingsRow(stringResource(R.string.setup_account), onClick = onAccount, icon = painterResource(R.drawable.ic_baseline_account_circle_24))
            SettingsRow(stringResource(R.string.setup_app_version), onClick = onAppInfo, icon = painterResource(R.drawable.ic_baseline_info_24))
            SettingsRow(stringResource(R.string.setup_notice), onClick = onNotice, icon = painterResource(R.drawable.ic_baseline_celebration_24))
            SettingsRow(stringResource(R.string.setup_friends), onClick = onSetFriends, icon = painterResource(R.drawable.ic_baseline_person_24))
            SettingsRow(stringResource(R.string.setup_lock), onClick = onLockSettings, icon = painterResource(R.drawable.ic_baseline_lock_24))
        }
    }
}
