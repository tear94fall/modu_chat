package com.example.modumessenger.core.util

import com.example.modumessenger.R
import com.example.modumessenger.core.model.ChatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatPreviewTest {

    private val storedJpg = "760ffc7db9c0c6033a4293139fdf8bd862d974cf06d98424fb7324e7779475e8.jpg"

    @Test
    fun `소켓으로 받은 사진 메시지는 파일 이름 대신 image 표식을 남긴다`() {
        assertEquals("image", ChatPreview.markerOf(ChatType.IMAGE, storedJpg))
        assertEquals("file", ChatPreview.markerOf(ChatType.FILE, "a.pdf"))
        assertEquals("audio", ChatPreview.markerOf(ChatType.AUDIO, "a.m4a"))
    }

    @Test
    fun `글 메시지는 본문 그대로다`() {
        assertEquals("안녕", ChatPreview.markerOf(ChatType.TEXT, "안녕"))
        assertEquals("안녕", ChatPreview.markerOf(ChatType.INVALID, "안녕"))
    }

    @Test
    fun `표식은 문구로 바뀐다`() {
        assertEquals(R.string.chat_preview_image, ChatPreview.resOf("image"))
        assertEquals(R.string.chat_preview_file, ChatPreview.resOf("file"))
        assertEquals(R.string.chat_preview_audio, ChatPreview.resOf("audio"))
    }

    @Test
    fun `표식 없이 저장 파일 이름이 남은 옛 방도 확장자로 알아낸다`() {
        assertEquals(R.string.chat_preview_image, ChatPreview.resOf(storedJpg))
        assertEquals(R.string.chat_preview_image, ChatPreview.resOf(storedJpg.replace(".jpg", ".PNG")))
        assertEquals(R.string.chat_preview_audio, ChatPreview.resOf(storedJpg.replace(".jpg", ".m4a")))
        assertEquals(R.string.chat_preview_file, ChatPreview.resOf(storedJpg.replace(".jpg", ".pdf")))
    }

    @Test
    fun `보통 글은 그대로 보여 준다`() {
        assertNull(ChatPreview.resOf("사진 보내 줄게 image.jpg"))
        assertNull(ChatPreview.resOf("abc.jpg"))
        assertNull(ChatPreview.resOf(""))
    }
}
