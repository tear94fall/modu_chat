package com.example.modumessenger.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 상태 바 자리에 까는 상단바 색 띠.
 *
 * Android 15(targetSdk 35)부터 상태 바가 항상 투명이라 테마의 statusBarColor 는 무시된다. 상단바가 없는 화면
 * (프로필 보기·편집, 채팅방 드로어)은 밝은 내용이 상태 바 뒤까지 비쳐 흰 시계·아이콘이 안 보이므로,
 * 기존 앱의 불투명 보라 상태 바처럼 이 띠를 맨 위에 둔다. Scaffold 의 topBar 로 쓰면 내용은 그 아래에서 시작한다.
 */
@Composable
fun StatusBarBand(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.primary),
    )
}
