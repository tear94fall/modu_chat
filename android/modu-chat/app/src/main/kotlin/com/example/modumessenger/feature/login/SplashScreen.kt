package com.example.modumessenger.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.modumessenger.core.ui.components.CenteredBranding
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * 앱을 열면 처음 보이는 화면(부록 A §1). 흰 바탕에 로고·앱 이름·소개 한 줄로,
 * 시스템 스플래시(`Theme.Modu.Splash`)가 그리던 그림을 그대로 이어 받는다.
 *
 * 세션 판정이 아무리 빨라도 [MIN_VISIBLE_MS] 동안은 머문다. 판정이 더 오래 걸리면 끝날 때까지 기다린다.
 */
@Composable
fun SplashScreen(
    awaitLoggedIn: suspend () -> Boolean,
    onDecided: (Boolean) -> Unit,
) {
    LaunchedEffect(Unit) {
        val loggedIn = coroutineScope {
            val decision = async { awaitLoggedIn() }
            delay(MIN_VISIBLE_MS)
            decision.await()
        }
        onDecided(loggedIn)
    }

    // 로고는 시스템 스플래시와 같은 자리(창 정중앙)에 둔다. 넘어가는 순간 로고가 움직이지 않는다.
    CenteredBranding(modifier = Modifier.fillMaxSize().background(Color.White))
}

/** 브랜딩이 눈에 들어올 만큼은 머문다. */
private const val MIN_VISIBLE_MS = 700L
