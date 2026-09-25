package com.example.modumessenger.core.util

import com.example.modumessenger.R
import com.example.modumessenger.core.model.ChatType

/**
 * 채팅방 목록의 마지막 메시지 미리보기.
 *
 * 서버(ws-service)는 사진·파일·음성 메시지의 방 `lastChatMsg` 에 본문(저장 파일 이름) 대신
 * `image` / `file` / `audio` 표식을 넣는다. 앱이 소켓으로 받은 메시지로 목록을 갱신할 때도
 * 같은 표식을 넣어야 목록에 파일 이름이 뜨지 않는다([markerOf]).
 *
 * 표식 없이 파일 이름이 그대로 저장된 옛 방도 있어서, 저장소 파일 이름 꼴(64자리 16진수 + 확장자)이면
 * 확장자로 종류를 알아낸다([resOf]).
 */
object ChatPreview {

    const val IMAGE = "image"
    const val FILE = "file"
    const val AUDIO = "audio"

    private val STORED_FILE = Regex("^[0-9a-f]{64}\\.([a-z0-9]+)$", RegexOption.IGNORE_CASE)
    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
    private val AUDIO_EXT = setOf("m4a", "mp3", "aac", "wav", "ogg", "amr", "3gp")

    /** 방 목록에 저장할 마지막 메시지. 글이면 본문 그대로, 아니면 종류 표식. */
    fun markerOf(chatType: Int, message: String): String = when (chatType) {
        ChatType.IMAGE -> IMAGE
        ChatType.FILE -> FILE
        ChatType.AUDIO -> AUDIO
        else -> message
    }

    /** 표식(또는 저장 파일 이름)이면 보여 줄 문구의 리소스, 글이면 null. */
    fun resOf(lastChatMsg: String): Int? {
        when (lastChatMsg) {
            IMAGE -> return R.string.chat_preview_image
            FILE -> return R.string.chat_preview_file
            AUDIO -> return R.string.chat_preview_audio
        }
        val ext = STORED_FILE.find(lastChatMsg)?.groupValues?.get(1)?.lowercase() ?: return null
        return when (ext) {
            in IMAGE_EXT -> R.string.chat_preview_image
            in AUDIO_EXT -> R.string.chat_preview_audio
            else -> R.string.chat_preview_file
        }
    }
}
