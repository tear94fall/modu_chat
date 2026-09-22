package com.example.modumessenger.feature.chat

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * 사진 말풍선 크기 규칙. 사진 비율을 지킨 채 최대 상자([MAX_WIDTH] × [MAX_HEIGHT]) 안에 맞춘다 —
 * 가로 사진은 가로가, 세로 사진은 세로가 상자에 닿고 다른 변은 비율대로 줄어든다. 그래서 잘리는 부분이 없다.
 *
 * 비율이 극단적인 사진(파노라마, 긴 캡처)만 [MIN_RATIO]~[MAX_RATIO] 로 제한한다. 그대로 두면 말풍선이
 * 가는 띠가 되어 뭐가 찍혔는지 알 수 없다. 제한된 만큼은 가운데를 잘라 보여 주고, 원본은 전체 보기에서 본다.
 * 짧은 변은 [MIN_SIDE] 아래로 내려가지 않는다.
 */
object ImageBubbleSize {
    val MAX_WIDTH: Dp = 220.dp
    val MAX_HEIGHT: Dp = 260.dp
    val MIN_SIDE: Dp = 100.dp
    const val MIN_RATIO = 0.4f
    const val MAX_RATIO = 2.5f

    /** 크기를 아직 모를 때(로딩 전) 자리를 잡아 두는 상자. 사진 대부분이 4:3 가로라 그 크기다. */
    val PLACEHOLDER: DpSize = of(4, 3)

    /** 사진의 픽셀 크기로 말풍선 상자 크기를 정한다. 크기를 모르면([PLACEHOLDER] 용) 4:3 로 본다. */
    fun of(widthPx: Int, heightPx: Int): DpSize {
        if (widthPx <= 0 || heightPx <= 0) return PLACEHOLDER
        val ratio = (widthPx.toFloat() / heightPx).coerceIn(MIN_RATIO, MAX_RATIO)
        val boxRatio = MAX_WIDTH / MAX_HEIGHT
        val fitted = if (ratio >= boxRatio) {
            DpSize(MAX_WIDTH, MAX_WIDTH / ratio)
        } else {
            DpSize(MAX_HEIGHT * ratio, MAX_HEIGHT)
        }
        return DpSize(
            width = maxOf(fitted.width, MIN_SIDE),
            height = maxOf(fitted.height, MIN_SIDE),
        )
    }
}
