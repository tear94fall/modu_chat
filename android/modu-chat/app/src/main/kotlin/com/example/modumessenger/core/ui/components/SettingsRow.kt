package com.example.modumessenger.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.modumessenger.core.ui.theme.BrandViolet

/**
 * 설정 계열 화면(설정 메뉴, 친구 설정, 화면 잠금, 계정 설정, 버전 정보)이 공유하는 한 줄.
 * 높이·여백·아이콘 원(옅은 보라 원 위의 보라 아이콘)이 전부 같아서 어느 화면을 열어도 같은 결이 난다.
 * [value] 는 오른쪽 끝의 현재 값(유예 시간 "즉시", 앱 버전 등), [trailing] 은 스위치처럼 값 대신 놓는 것.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    icon: Painter? = null,
    iconVector: ImageVector? = null,
    subtitle: String? = null,
    value: String? = null,
    titleColor: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SETTINGS_ROW_HEIGHT)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = SETTINGS_ROW_HORIZONTAL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SETTINGS_ROW_GAP),
    ) {
        if (icon != null || iconVector != null) {
            Box(
                modifier = Modifier
                    .size(SETTINGS_ICON_CIRCLE)
                    .background(BrandViolet.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Icon(painter = icon, contentDescription = null, modifier = Modifier.size(SETTINGS_ICON_SIZE), tint = BrandViolet)
                } else if (iconVector != null) {
                    Icon(imageVector = iconVector, contentDescription = null, modifier = Modifier.size(SETTINGS_ICON_SIZE), tint = BrandViolet)
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor,
            )
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

/** 오른쪽에 스위치가 있는 줄. 줄 전체를 눌러도 토글된다. */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    iconVector: ImageVector? = null,
    subtitle: String? = null,
) {
    SettingsRow(
        title = title,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        icon = icon,
        iconVector = iconVector,
        subtitle = subtitle,
        // Material3 스위치(52×32dp)는 이 행에 크다. 크기 속성이 없어 스케일로 줄인다.
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(SWITCH_SCALE),
            )
        },
    )
}

val SETTINGS_ROW_HEIGHT = 76.dp
private val SETTINGS_ROW_HORIZONTAL = 20.dp
private val SETTINGS_ROW_GAP = 16.dp
private val SETTINGS_ICON_CIRCLE = 36.dp
private val SETTINGS_ICON_SIZE = 20.dp
private const val SWITCH_SCALE = 0.8f
