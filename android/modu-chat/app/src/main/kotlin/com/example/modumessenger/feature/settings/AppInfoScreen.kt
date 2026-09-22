package com.example.modumessenger.feature.settings

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.modumessenger.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import com.example.modumessenger.core.ui.components.ModuTopBar
import com.example.modumessenger.core.ui.components.SettingsRow

/**
 * 버전 정보(부록 A §24). 기존 앱과 같은 순서로 서버/앱/os 세 줄을 그린다.
 * 서버 버전은 받아오기 전이거나 서버에 값이 없으면 빈 값으로 남는다.
 */
@Composable
fun AppInfoScreen(
    onBack: () -> Unit,
    viewModel: AppInfoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    val serverVersion by viewModel.serverVersion.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ModuTopBar(title = stringResource(R.string.app_info_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.modu_logo),
                contentDescription = null,
                modifier = Modifier.padding(vertical = 24.dp).size(96.dp),
            )
            SettingsRow(
                title = stringResource(R.string.app_info_server_version),
                iconVector = Icons.Filled.Cloud,
                value = serverVersion,
            )
            SettingsRow(
                title = stringResource(R.string.app_info_app_version),
                iconVector = Icons.Filled.PhoneAndroid,
                value = versionName,
            )
            SettingsRow(
                title = stringResource(R.string.app_info_os_version),
                iconVector = Icons.Filled.Android,
                value = stringResource(R.string.app_info_os_value, Build.VERSION.RELEASE ?: ""),
            )
        }
    }
}
