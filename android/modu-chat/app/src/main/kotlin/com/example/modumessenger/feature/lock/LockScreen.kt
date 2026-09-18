package com.example.modumessenger.feature.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.modumessenger.R
import com.example.modumessenger.core.lock.AppLock
import com.example.modumessenger.core.ui.components.ConfirmDialog
import com.example.modumessenger.core.ui.theme.BrandViolet
import com.example.modumessenger.core.ui.theme.ModuRed

/**
 * 잠금 화면. NavHost 위를 덮는 전체 화면이라 뒤로 가기는 [onBack] 이 정한다(메인은 앱을 뒤로,
 * SSO 는 취소). "PIN 을 잊으셨나요?" 로그아웃이 끝나면 [onLoggedOut] 을 부른다.
 */
@Composable
fun LockScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LockViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometricAvailable = remember(context) { Biometrics.isAvailable(context) }
    val showBiometric = state.biometricEnabled && biometricAvailable && activity != null
    var showForgot by remember { mutableStateOf(false) }

    val promptTitle = stringResource(R.string.lock_biometric_prompt_title)
    val promptNegative = stringResource(R.string.lock_biometric_prompt_negative)
    val prompt = {
        if (activity != null) {
            Biometrics.prompt(activity, promptTitle, promptNegative, viewModel::onBiometricSuccess, onDismiss = {})
        }
    }

    BackHandler(onBack = onBack)

    // 뷰모델은 액티비티 수명이라 잠금 사이에 살아남는다. 화면이 다시 보일 때 입력 찌꺼기를 비운다.
    LaunchedEffect(Unit) { viewModel.reset() }
    LaunchedEffect(showBiometric) { if (showBiometric) prompt() }
    LaunchedEffect(viewModel) { viewModel.loggedOut.collect { onLoggedOut() } }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(HEADER_TOP))
                Image(
                    painter = painterResource(R.drawable.modu_logo),
                    contentDescription = null,
                    modifier = Modifier.size(LOGO_SIZE),
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.lock_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        state.lockedOutSeconds > 0 -> stringResource(R.string.lock_locked_out, state.lockedOutSeconds)
                        state.attemptsLeft != null ->
                            stringResource(R.string.lock_wrong_pin, AppLock.MAX_ATTEMPTS - state.attemptsLeft!!, AppLock.MAX_ATTEMPTS)
                        else -> stringResource(R.string.lock_enter_pin)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.lockedOutSeconds > 0 || state.attemptsLeft != null) ModuRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(HEADER_TO_PAD))

            PinPad(
                pin = state.pin,
                enabled = state.inputEnabled,
                shakeKey = state.shakeKey,
                onDigit = viewModel::onDigit,
                onDelete = viewModel::onDelete,
                leftSlot = if (showBiometric) {
                    {
                        IconButton(onClick = prompt, enabled = state.inputEnabled) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = stringResource(R.string.lock_biometric),
                                tint = BrandViolet,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                } else {
                    null
                },
            )

            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { showForgot = true },
                enabled = !state.working,
                modifier = Modifier.padding(bottom = FORGOT_BOTTOM),
            ) {
                Text(stringResource(R.string.lock_forgot), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showForgot) {
        ConfirmDialog(
            title = stringResource(R.string.lock_forgot_title),
            text = stringResource(R.string.lock_forgot_message),
            confirmText = stringResource(R.string.lock_forgot_confirm),
            dismissText = stringResource(R.string.lock_cancel),
            onConfirm = {
                showForgot = false
                viewModel.forgot()
            },
            onDismiss = { showForgot = false },
            confirmColor = ModuRed,
        )
    }
}

private val LOGO_SIZE = 72.dp

/** 로고·문구는 화면 위쪽 1/5 즈음, 키패드는 그 바로 아래, 잊음 버튼은 하단바에서 떨어뜨린다. */
private val HEADER_TOP = 112.dp
private val HEADER_TO_PAD = 36.dp
private val FORGOT_BOTTOM = 72.dp
