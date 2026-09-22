package com.example.modumessenger.feature.chat

import android.net.Uri
import com.example.modumessenger.data.api.ChatApi
import com.example.modumessenger.data.api.ChatRoomApi
import com.example.modumessenger.data.dto.ChatDto
import com.example.modumessenger.data.dto.ChatReadCursorDto
import com.example.modumessenger.data.dto.ChatRoomDto
import com.example.modumessenger.data.dto.ChatRoomUnreadDto
import com.example.modumessenger.core.model.AudioPlayback
import com.example.modumessenger.core.model.DownloadedFile
import com.example.modumessenger.core.model.FileInfo
import com.example.modumessenger.data.repository.AttachmentRepository
import com.example.modumessenger.data.repository.StorageRepository
import com.example.modumessenger.feature.chat.audio.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import com.example.modumessenger.data.socket.ChatSocket
import com.example.modumessenger.data.socket.ConnectionState
import com.example.modumessenger.data.socket.SocketEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** 보낸 프레임만 모아 두는 가짜 소켓. [sendResult] 로 전송 실패를 흉내 낸다. */
class FakeChatSocket : ChatSocket {

    val incoming = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val sent = mutableListOf<String>()
    var sendResult = true

    override val state = MutableStateFlow(ConnectionState.CONNECTED)
    override val events: SharedFlow<SocketEvent> = incoming

    override fun connect() = Unit
    override fun disconnect() = Unit

    override fun send(text: String): Boolean {
        if (sendResult) sent += text
        return sendResult
    }
}

class FakeChatApi : ChatApi {
    var recent: List<ChatDto> = emptyList()
    var before: List<ChatDto> = emptyList()
    var images: List<ChatDto> = emptyList()
    var byIds: List<ChatDto> = emptyList()

    override suspend fun getChats(ids: List<String>): List<ChatDto> = byIds
    override suspend fun getRecent(roomId: String, size: Int): List<ChatDto> = recent
    override suspend fun getBefore(roomId: String, chatId: Long, size: Int): List<ChatDto> = before
    override suspend fun getImages(roomId: String, size: Int): List<ChatDto> = images
}

class FakeChatRoomApi : ChatRoomApi {
    var rooms: List<ChatRoomDto> = emptyList()
    var unread: List<ChatRoomUnreadDto> = emptyList()
    var cursors: List<ChatReadCursorDto> = emptyList()

    override suspend fun getRooms(memberId: String): List<ChatRoomDto> = rooms
    override suspend fun getRoom(roomId: String): ChatRoomDto = rooms.first { it.roomId == roomId }
    override suspend fun createRoom(ids: List<Long>): ChatRoomDto = rooms.first()
    override suspend fun leaveRoom(roomId: String, userId: String): ChatRoomDto =
        rooms.first { it.roomId == roomId }

    override suspend fun updateRoom(roomId: String, room: ChatRoomDto): ChatRoomDto = room
    override suspend fun inviteMembers(roomId: String, userIds: List<String>): ChatRoomDto =
        rooms.first { it.roomId == roomId }

    override suspend fun getUnreadCounts(memberId: String): List<ChatRoomUnreadDto> = unread
    override suspend fun updateLastRead(roomId: String, memberId: String) = Unit
    override suspend fun getReadCursors(roomId: String): List<ChatReadCursorDto> = cursors
}

/** 업로드는 언제나 같은 파일 이름을 돌려준다. */
class FakeStorageRepository : StorageRepository {
    var uploadResult: Result<String> = Result.success("uploaded.jpg")

    override suspend fun upload(uri: Uri): Result<String> = uploadResult
    override fun takePictureUri(): Uri = Uri.EMPTY
}

/** 파일 정보는 저장 이름 그대로, 내려받기·캐시는 실패 없이 끝난다. */
class FakeAttachmentRepository : AttachmentRepository {
    var infos: Map<String, FileInfo> = emptyMap()
    val downloaded = mutableListOf<String>()

    override suspend fun fileInfo(name: String): Result<FileInfo> =
        Result.success(infos[name] ?: FileInfo(name, name, -1L, "application/octet-stream"))

    override suspend fun saveToDownloads(name: String): Result<DownloadedFile> {
        downloaded += name
        return Result.success(DownloadedFile(infos[name]?.originalName ?: name, Uri.EMPTY))
    }

    override suspend fun findDownloaded(info: FileInfo): DownloadedFile? = null
    override fun open(file: DownloadedFile, contentType: String): Boolean = true
    override suspend fun cacheFile(name: String): Result<File> = Result.success(File(name))
    override suspend fun audioDuration(name: String): Result<Long> = Result.success(10_000L)
}

/** 상태만 기억하는 재생기. */
class FakeAudioPlayer : AudioPlayer {
    private val _state = MutableStateFlow<AudioPlayback?>(null)
    override val state: StateFlow<AudioPlayback?> = _state

    override fun toggle(fileName: String) {
        val current = _state.value
        _state.value = if (current?.fileName == fileName) {
            current.copy(isPlaying = !current.isPlaying)
        } else {
            AudioPlayback(fileName = fileName, isPlaying = true, durationMs = 10_000L)
        }
    }

    override fun pause() { _state.value = _state.value?.copy(isPlaying = false) }
    override fun seekTo(positionMs: Long) { _state.value = _state.value?.copy(positionMs = positionMs) }
    override fun toggleMute() { _state.value = _state.value?.copy(isMuted = !(_state.value?.isMuted ?: false)) }
    override fun stop() { _state.value = null }
}
