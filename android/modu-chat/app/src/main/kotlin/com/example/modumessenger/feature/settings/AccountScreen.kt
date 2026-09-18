package com.example.modumessenger.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.modumessenger.R
import com.example.modumessenger.core.ui.components.ConfirmDialog
import com.example.modumessenger.core.ui.components.ModuTopBar
import com.example.modumessenger.core.ui.theme.ModuRed
import com.example.modumessenger.feature.login.GoogleSignInHelper

/** 계정 설정(부록 A §22). 버튼 하나, 확인 팝업, 그리고 로그인 화면으로. */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { res ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(res))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.loggedOut.collect {
            // 다음 로그인 때 무음 로그인이 옛 계정으로 붙지 않도록 구글 쪽도 끊는다.
            GoogleSignInHelper.signOut(context)
            onLoggedOut()
        }
    }

    Scaffold(
        topBar = { ModuTopBar(title = stringResource(R.string.account_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { showDialog = true },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.account_logout))
            }
            // 탈퇴는 되돌릴 수 없는 동작이라 빨간 테두리 버튼으로 로그아웃과 구분한다.
            OutlinedButton(
                onClick = { showWithdrawDialog = true },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ModuRed),
                border = BorderStroke(1.dp, ModuRed),
            ) {
                Text(stringResource(R.string.account_withdraw))
            }
        }
    }

    if (showWithdrawDialog) {
        ConfirmDialog(
            title = stringResource(R.string.account_withdraw),
            text = stringResource(R.string.account_withdraw_message),
            confirmText = stringResource(R.string.account_withdraw_confirm),
            dismissText = stringResource(R.string.account_cancel),
            onConfirm = {
                showWithdrawDialog = false
                viewModel.withdraw()
            },
            onDismiss = { showWithdrawDialog = false },
            confirmColor = ModuRed,
        )
    }

    if (showDialog) {
        ConfirmDialog(
            title = stringResource(R.string.account_logout),
            text = stringResource(R.string.account_logout_message),
            confirmText = stringResource(R.string.account_logout),
            dismissText = stringResource(R.string.account_cancel),
            onConfirm = {
                showDialog = false
                viewModel.logout()
            },
            onDismiss = { showDialog = false },
            confirmColor = ModuRed,
        )
    }
}
