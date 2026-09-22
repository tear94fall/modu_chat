package com.example.modumessenger.core.util

import com.example.modumessenger.core.model.ChatType
import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentTextTest {

    @Test
    fun `파일 크기는 1024 단위로 한 자리 소수`() {
        assertEquals("0 B", FileSizeText.of(0))
        assertEquals("512 B", FileSizeText.of(512))
        assertEquals("1.0 KB", FileSizeText.of(1024))
        assertEquals("1.2 KB", FileSizeText.of(1234))
        assertEquals("1.5 MB", FileSizeText.of(1_572_864))
        assertEquals("2.0 GB", FileSizeText.of(2L * 1024 * 1024 * 1024))
        assertEquals("", FileSizeText.of(-1))
    }

    @Test
    fun `재생 시각은 분초로, 한 시간을 넘으면 시분초`() {
        assertEquals("0:00", AudioTime.of(0))
        assertEquals("0:12", AudioTime.of(12_300))
        assertEquals("1:05", AudioTime.of(65_000))
        assertEquals("1:00:01", AudioTime.of(3_601_000))
        assertEquals("0:00", AudioTime.of(-5_000))
    }

    @Test
    fun `푸시 본문은 사진·파일·음성이면 고정 문구`() {
        assertEquals("새로운 사진", NotificationText.bodyOf(ChatType.IMAGE, "abc.jpg"))
        assertEquals("새로운 파일", NotificationText.bodyOf(ChatType.FILE, "abc.xml"))
        assertEquals("새로운 음성", NotificationText.bodyOf(ChatType.AUDIO, "abc.m4a"))
        assertEquals("안녕", NotificationText.bodyOf(ChatType.TEXT, "안녕"))
        assertEquals("", NotificationText.bodyOf(ChatType.TEXT, null))
    }
}
