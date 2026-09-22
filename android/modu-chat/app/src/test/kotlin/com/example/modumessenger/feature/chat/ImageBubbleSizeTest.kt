package com.example.modumessenger.feature.chat

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** 사진 말풍선 크기 규칙: 비율 유지 + 최대 상자 안에 맞춤 + 극단 비율만 제한. */
class ImageBubbleSizeTest {

    private fun assertSize(expectedWidth: Float, expectedHeight: Float, actual: DpSize) {
        assertEquals(expectedWidth, actual.width.value, 0.5f)
        assertEquals(expectedHeight, actual.height.value, 0.5f)
    }

    @Test
    fun `가로 사진은 가로가 상자에 닿고 세로는 비율대로 줄어든다`() {
        assertSize(220f, 165f, ImageBubbleSize.of(1200, 900)) // 4:3
        assertSize(220f, 146.8f, ImageBubbleSize.of(1199, 800)) // 3:2
    }

    @Test
    fun `세로 사진은 세로가 상자에 닿고 가로는 비율대로 줄어든다`() {
        assertSize(195f, 260f, ImageBubbleSize.of(900, 1200)) // 3:4
        assertSize(146.25f, 260f, ImageBubbleSize.of(1080, 1920)) // 9:16 캡처
    }

    @Test
    fun `정사각형은 짧은 쪽인 가로에 맞춘다`() {
        assertSize(220f, 220f, ImageBubbleSize.of(1000, 1000))
        assertSize(220f, 230.6f, ImageBubbleSize.of(620, 650))
    }

    @Test
    fun `극단적인 비율은 제한되고 짧은 변이 최소 크기 아래로 내려가지 않는다`() {
        // 파노라마 5:1 → 2.5:1 로 제한 → 220×88 → 세로 최소 100
        assertSize(220f, 100f, ImageBubbleSize.of(5000, 1000))
        // 긴 캡처 1:5 → 1:2.5 로 제한 → 104×260
        assertSize(104f, 260f, ImageBubbleSize.of(1000, 5000))
    }

    @Test
    fun `크기를 모르면 4대3 자리 상자를 쓴다`() {
        assertEquals(ImageBubbleSize.PLACEHOLDER, ImageBubbleSize.of(0, 0))
        assertSize(220f, 165f, ImageBubbleSize.PLACEHOLDER)
        assertEquals(220.dp, ImageBubbleSize.MAX_WIDTH)
    }
}
