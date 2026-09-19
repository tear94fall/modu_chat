package com.example.modumessenger.feature.lock

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modumessenger.R
import com.example.modumessenger.core.lock.AppLock
import com.example.modumessenger.core.ui.theme.BrandViolet
import kotlin.math.roundToInt

/**
 * PIN 점 4개와 3×4 숫자 키패드. 잠금 화면과 잠금 설정(PIN 입력·확인)이 같이 쓴다.
 * [shakeKey] 가 바뀌면 점 줄이 좌우로 흔들린다(틀렸다는 신호). [leftSlot] 은 왼쪽 아래 칸(지문 버튼 자리).
 */
@Composable
fun PinPad(
    pin: String,
    enabled: Boolean,
    shakeKey: Int,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    leftSlot: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current
    // 키패드는 진짜 키보드처럼 눌릴 때마다 짧게 떨린다.
    val haptic = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0) return@LaunchedEffect
        repeat(3) {
            shake.animateTo(SHAKE_PX, tween(SHAKE_STEP_MS))
            shake.animateTo(-SHAKE_PX, tween(SHAKE_STEP_MS))
        }
        shake.animateTo(0f, tween(SHAKE_STEP_MS))
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
            horizontalArrangement = Arrangement.spacedBy(DOT_GAP),
        ) {
            repeat(AppLock.PIN_LENGTH) { index ->
                val filled = index < pin.length
                // 채워질 때 살짝 커졌다 돌아오고 색이 스며든다. 4번째 점의 이 움직임이 보인 뒤에 검증이 돈다.
                val scale by animateFloatAsState(
                    targetValue = if (filled) 1f else 0.85f,
                    animationSpec = spring(dampingRatio = 0.45f, stiffness = 900f),
                    label = "dotScale",
                )
                val fill by animateColorAsState(
                    targetValue = if (filled) BrandViolet else MaterialTheme.colorScheme.surface,
                    animationSpec = tween(DOT_FILL_MS),
                    label = "dotFill",
                )
                val outline by animateColorAsState(
                    targetValue = if (filled) BrandViolet else MaterialTheme.colorScheme.outline,
                    animationSpec = tween(DOT_FILL_MS),
                    label = "dotOutline",
                )
                Box(
                    modifier = Modifier
                        .size(DOT_SIZE)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(fill)
                        .border(2.dp, outline, CircleShape),
                )
            }
        }

        Column(
            modifier = Modifier.offset(y = KEYPAD_TOP),
            verticalArrangement = Arrangement.spacedBy(KEY_GAP),
        ) {
            listOf("123", "456", "789").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                    row.forEach { digit ->
                        DigitKey(digit, enabled) {
                            haptic()
                            onDigit(digit)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                Box(modifier = Modifier.size(KEY_SIZE), contentAlignment = Alignment.Center) {
                    leftSlot?.invoke()
                }
                DigitKey('0', enabled) {
                    haptic()
                    onDigit('0')
                }
                Box(
                    modifier = Modifier
                        .size(KEY_SIZE)
                        .clip(CircleShape)
                        .clickable(enabled = enabled) {
                            haptic()
                            onDelete()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.lock_delete_digit),
                        tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Char, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(KEY_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit.toString(),
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        )
    }
}

private val DOT_SIZE = 16.dp
private val DOT_GAP = 20.dp
private val KEY_SIZE = 72.dp
private val KEY_GAP = 16.dp
private val KEYPAD_TOP = 40.dp
private const val SHAKE_PX = 24f
private const val DOT_FILL_MS = 120
private const val SHAKE_STEP_MS = 45
