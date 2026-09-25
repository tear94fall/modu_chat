package com.example.modumessenger.feature.chat.audio

import com.example.modumessenger.core.model.AudioPlayback
import kotlinx.coroutines.flow.StateFlow

/**
 * 음성 메시지 재생기. 앱에 하나만 있고 한 번에 한 파일만 튼다.
 * 말풍선은 [state] 의 파일 이름이 자기 것일 때만 진행 상태를 그린다.
 */
interface AudioPlayer {

    val state: StateFlow<AudioPlayback?>

    /** 같은 파일이 재생 중이면 멈추고, 멈춰 있으면 이어서 틀고, 다른 파일이면 새로 튼다. */
    fun toggle(fileName: String)

    fun pause()

    fun seekTo(positionMs: Long)

    fun toggleMute()

    /** 재생을 끝내고 상태를 비운다. */
    fun stop()
}
