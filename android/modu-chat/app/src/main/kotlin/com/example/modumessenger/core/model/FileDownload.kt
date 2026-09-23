package com.example.modumessenger.core.model

import android.net.Uri

/** 내려받은 파일. [uri] 로 다른 앱에서 연다. */
data class DownloadedFile(val displayName: String, val uri: Uri)

/**
 * 파일 말풍선의 내려받기 상태. 받는 중에는 다시 눌러도 무시하고, 받은 뒤에는 누르면 다시 받지 않고 연다.
 * 앱을 다시 켜도 Downloads 에 같은 이름·크기의 파일이 있으면 [DONE] 으로 본다.
 */
data class FileDownloadUi(
    val state: State = State.IDLE,
    val file: DownloadedFile? = null,
) {
    enum class State { IDLE, DOWNLOADING, DONE }
}
