package com.example.modumessenger.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 원형 아이콘 버튼. 기존 앱의 `circle_icon_button*` 배경을 깐 ImageButton 과 같은 모양이다.
 *
 * Material3 IconButton 은 최소 터치 영역(48dp)을 원 바깥에 더해 원이 지정한 크기보다 커 보인다.
 * 원 크기를 그대로 지키려고 Box 로 그린다.
 */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = SCRIM_BUTTON_SIZE,
    iconSize: Dp = SCRIM_ICON_SIZE,
    background: Color = SCRIM_BACKGROUND,
    tint: Color = Color.White,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** 사진 위에 얹는 반투명 검정 원(`circle_icon_button_scrim`). 프로필 보기·편집이 같이 쓴다. */
val SCRIM_BUTTON_SIZE = 36.dp
val SCRIM_ICON_SIZE = 20.dp
val SCRIM_BACKGROUND = Color(0x59000000)
