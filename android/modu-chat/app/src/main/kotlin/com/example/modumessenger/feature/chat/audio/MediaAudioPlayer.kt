package com.example.modumessenger.feature.chat.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.modumessenger.core.di.ApplicationScope
import com.example.modumessenger.core.model.AudioPlayback
import com.example.modumessenger.data.repository.AttachmentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MediaPlayer] 기반 재생기. 음성은 인증이 필요한 주소라 [AttachmentRepository.cacheFile] 로 먼저 받아 두고
 * 로컬 파일을 튼다 — 그래야 좌우 이동(seek)도 즉시 된다. 모든 MediaPlayer 호출은 메인 스레드에서 한다.
 */
@Singleton
class MediaAudioPlayer @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : AudioPlayer {

    private val _state = MutableStateFlow<AudioPlayback?>(null)
    override val state: StateFlow<AudioPlayback?> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var loadJob: Job? = null
    private var tickerJob: Job? = null

    override fun toggle(fileName: String) {
        val current = _state.value
        val mp = player
        if (current != null && current.fileName == fileName && mp != null && !current.isLoading && !current.failed) {
            if (mp.isPlaying) {
                mp.pause()
                stopTicker()
                _state.update { it?.copy(isPlaying = false, positionMs = mp.currentPosition.toLong()) }
            } else {
                mp.start()
                _state.update { it?.copy(isPlaying = true) }
                startTicker()
            }
            return
        }
        load(fileName, muted = current?.isMuted ?: false)
    }

    private fun load(fileName: String, muted: Boolean) {
        release()
        _state.value = AudioPlayback(fileName = fileName, isLoading = true, isMuted = muted)
        loadJob = scope.launch(Dispatchers.Main.immediate) {
            val file = attachmentRepository.cacheFile(fileName).getOrElse {
                _state.update { s -> s?.takeIf { it.fileName == fileName }?.copy(isLoading = false, failed = true) }
                return@launch
            }
            val mp = MediaPlayer()
            try {
                withContext(Dispatchers.IO) {
                    mp.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    mp.setDataSource(file.absolutePath)
                    mp.prepare()
                }
            } catch (e: Exception) {
                mp.release()
                _state.update { s -> s?.takeIf { it.fileName == fileName }?.copy(isLoading = false, failed = true) }
                return@launch
            }
            // 받는 동안 다른 파일로 바뀌었으면 버린다.
            if (_state.value?.fileName != fileName) {
                mp.release()
                return@launch
            }
            player = mp
            applyMute(mp, muted)
            mp.setOnCompletionListener {
                stopTicker()
                _state.update { s -> s?.copy(isPlaying = false, positionMs = s.durationMs) }
            }
            mp.start()
            _state.value = AudioPlayback(
                fileName = fileName,
                isPlaying = true,
                positionMs = 0L,
                durationMs = mp.duration.toLong().coerceAtLeast(0L),
                isMuted = muted,
            )
            startTicker()
        }
    }

    override fun pause() {
        val mp = player ?: return
        if (mp.isPlaying) mp.pause()
        stopTicker()
        _state.update { it?.copy(isPlaying = false, positionMs = mp.currentPosition.toLong()) }
    }

    override fun seekTo(positionMs: Long) {
        val mp = player ?: return
        val duration = _state.value?.durationMs ?: 0L
        val target = positionMs.coerceIn(0L, duration)
        mp.seekTo(target.toInt())
        _state.update { it?.copy(positionMs = target) }
    }

    override fun toggleMute() {
        val muted = !(_state.value?.isMuted ?: false)
        player?.let { applyMute(it, muted) }
        _state.update { it?.copy(isMuted = muted) }
    }

    override fun stop() {
        release()
        _state.value = null
    }

    private fun applyMute(mp: MediaPlayer, muted: Boolean) {
        val volume = if (muted) 0f else 1f
        mp.setVolume(volume, volume)
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                val mp = player ?: break
                _state.update { it?.copy(positionMs = mp.currentPosition.toLong()) }
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun release() {
        loadJob?.cancel()
        loadJob = null
        stopTicker()
        player?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            mp.release()
        }
        player = null
    }

    private companion object {
        const val TICK_MS = 200L
    }
}
