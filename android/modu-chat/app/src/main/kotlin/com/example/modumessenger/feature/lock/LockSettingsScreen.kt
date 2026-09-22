package com.example.modumessenger.feature.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.modumessenger.R
import com.example.modumessenger.core.lock.AppLock
import com.example.modumessenger.core.lock.LockGrace
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Timer
import com.example.modumessenger.core.ui.components.ModuTopBar
import com.example.modumessenger.core.ui.components.SettingsRow
import com.example.modumessenger.core.ui.components.SettingsSwitchRow
import com.example.modumessenger.core.ui.theme.ModuRed

/** 화면 잠금 설정. PIN 이 필요한 단계에서는 목록 대신 키패드를 보여 준다. */
@Composable
fun LockSettingsScreen(
    onBack: () -> Unit,
    viewModel: LockSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricAvailable = remember(context) { Biometrics.isAvailable(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showGraceDialog by remember { mutableStateOf(false) }
    val inStep = state.step != PinStep.NONE

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { res ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(res))
        }
    }

    // PIN 단계에서 뒤로 가기는 단계만 취소한다.
    BackHandler(enabled = inStep) { viewModel.cancelStep() }

    Scaffold(
        topBar = {
            ModuTopBar(
                title = stringResource(R.string.lock_settings_title),
                onBack = { if (inStep) viewModel.cancelStep() else onBack() },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (inStep) {
            PinStepContent(state = state, viewModel = viewModel, modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            SettingsList(
                state = state,
                biometricAvailable = biometricAvailable,
                onToggleEnabled = { if (it) viewModel.startEnable() else viewModel.startDisable() },
                onChangePin = viewModel::startChangePin,
                onToggleBiometric = { viewModel.setBiometric(it, biometricAvailable) },
                onGrace = { showGraceDialog = true },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }

    if (showGraceDialog) {
        GraceDialog(
            selected = state.settings.grace,
            onSelect = {
                viewModel.setGrace(it)
                showGraceDialog = false
            },
            onDismiss = { showGraceDialog = false },
        )
    }
}

@Composable
private fun PinStepContent(state: LockSettingsUiState, viewModel: LockSettingsViewModel, modifier: Modifier) {
    val title = when (state.step) {
        PinStep.ENABLE_NEW, PinStep.CHANGE_NEW -> R.string.lock_setup_new
        PinStep.ENABLE_CONFIRM, PinStep.CHANGE_CONFIRM -> R.string.lock_setup_confirm
        PinStep.DISABLE_VERIFY, PinStep.CHANGE_CURRENT -> R.string.lock_setup_current
        PinStep.NONE -> R.string.lock_enter_pin
    }
    val hint = when {
        state.lockedOutSeconds > 0 -> stringResource(R.string.lock_locked_out, state.lockedOutSeconds)
        state.attemptsLeft != null -> stringResource(R.string.lock_wrong_pin, AppLock.MAX_ATTEMPTS - state.attemptsLeft, AppLock.MAX_ATTEMPTS)
        state.hintRes != null -> stringResource(state.hintRes)
        else -> null
    }
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(text = stringResource(title), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = hint ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = ModuRed,
        )
        Spacer(modifier = Modifier.height(32.dp))
        PinPad(
            pin = state.pin,
            enabled = state.inputEnabled,
            shakeKey = state.shakeKey,
            onDigit = viewModel::onDigit,
            onDelete = viewModel::onDelete,
        )
    }
}

@Composable
private fun SettingsList(
    state: LockSettingsUiState,
    biometricAvailable: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onGrace: () -> Unit,
    modifier: Modifier,
) {
    val settings = state.settings
    Column(modifier = modifier) {
        SettingsSwitchRow(
            title = stringResource(R.string.lock_settings_enable),
            subtitle = stringResource(R.string.lock_settings_enable_desc),
            checked = settings.enabled,
            onCheckedChange = onToggleEnabled,
            iconVector = Icons.Filled.Lock,
        )
        if (!settings.enabled) return@Column
        SettingsRow(
            title = stringResource(R.string.lock_settings_change_pin),
            onClick = onChangePin,
            iconVector = Icons.Filled.Pin,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.lock_settings_biometric),
            subtitle = if (biometricAvailable) null else stringResource(R.string.lock_settings_biometric_unavailable),
            checked = settings.biometricEnabled && biometricAvailable,
            onCheckedChange = onToggleBiometric,
            iconVector = Icons.Filled.Fingerprint,
        )
        SettingsRow(
            title = stringResource(R.string.lock_settings_grace),
            onClick = onGrace,
            iconVector = Icons.Filled.Timer,
            value = stringResource(settings.grace.labelRes()),
        )
    }
}

@Composable
private fun GraceDialog(selected: LockGrace, onSelect: (LockGrace) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lock_settings_grace)) },
        text = {
            Column {
                LockGrace.entries.forEach { grace ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = grace == selected, onClick = { onSelect(grace) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = grace == selected, onClick = { onSelect(grace) })
                        Text(text = stringResource(grace.labelRes()), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.lock_cancel)) }
        },
    )
}

private fun LockGrace.labelRes(): Int = when (this) {
    LockGrace.IMMEDIATE -> R.string.lock_grace_immediate
    LockGrace.SECONDS_30 -> R.string.lock_grace_30s
    LockGrace.MINUTE_1 -> R.string.lock_grace_1m
    LockGrace.MINUTES_5 -> R.string.lock_grace_5m
}

