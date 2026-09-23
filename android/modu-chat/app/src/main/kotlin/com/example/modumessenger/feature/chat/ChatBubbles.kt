package com.example.modumessenger.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.modumessenger.core.model.AudioPlayback
import com.example.modumessenger.core.model.ChatType
import com.example.modumessenger.core.model.FileInfo
import com.example.modumessenger.core.util.AudioTime
import com.example.modumessenger.core.util.FileSizeText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import com.example.modumessenger.core.model.FileDownloadUi
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import android.view.HapticFeedbackConstants
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size
import com.example.modumessenger.R
import com.example.modumessenger.core.network.ApiConfig
import com.example.modumessenger.core.ui.components.ProfileImage

/**
 * 말풍선 한 줄(부록 A §8a). 왼쪽/오른쪽 구분과 묶음 위치에 따라 아바타·이름·시각이 나타났다 사라진다.
 */
@Composable
fun ChatBubbleRow(
    bubble: ChatBubble,
    onOpenProfile: (Long) -> Unit,
    onOpenImage: (Long) -> Unit,
    onResend: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onReact: (Long, String) -> Unit = { _, _ -> },
    onShowReactors: (Long) -> Unit = {},
    attachments: AttachmentUi = AttachmentUi(),
    attachmentActions: AttachmentActions = AttachmentActions(),
) {
    if (bubble.isMine) {
        RightBubble(bubble, onOpenImage, onResend, onDelete, onShowReactors, attachments, attachmentActions, modifier)
    } else {
        LeftBubble(bubble, onOpenProfile, onOpenImage, onReact, onShowReactors, attachments, attachmentActions, modifier)
    }
}

/** 파일·음성 말풍선이 그릴 상태. [fileInfos] 는 저장 이름 → 원본 이름·크기, [audio] 는 지금 재생 중인 음성. */
data class AttachmentUi(
    val fileInfos: Map<String, FileInfo> = emptyMap(),
    val fileDownloads: Map<String, FileDownloadUi> = emptyMap(),
    val audio: AudioPlayback? = null,
    val audioDurations: Map<String, Long> = emptyMap(),
)

/** 파일·음성 말풍선의 동작. 화면이 ViewModel 에 잇는다. */
class AttachmentActions(
    val onNeedFileInfo: (String) -> Unit = {},
    /** 안 받았으면 받고, 받았으면 연다. 받는 중이면 무시. */
    val onOpenOrDownload: (String) -> Unit = {},
    val onNeedAudioDuration: (String) -> Unit = {},
    val onToggleAudio: (String) -> Unit = {},
    val onSeekAudio: (Long) -> Unit = {},
    val onToggleMute: () -> Unit = {},
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LeftBubble(
    bubble: ChatBubble,
    onOpenProfile: (Long) -> Unit,
    onOpenImage: (Long) -> Unit,
    onReact: (Long, String) -> Unit,
    onShowReactors: (Long) -> Unit,
    attachments: AttachmentUi,
    actions: AttachmentActions,
    modifier: Modifier,
) {
    // 길게 누르면 말풍선 위에 이모지 바. 말풍선이 바뀌면(id) 상태도 새로.
    var showBar by remember(bubble.message.id) { mutableStateOf(false) }
    // 길게 누른 순간의 말풍선 세로 중심. 화면 위쪽 절반이면 바를 말풍선 아래에 띄운다(위에 띄우면 가려지거나 잘린다).
    var barBelow by remember(bubble.message.id) { mutableStateOf(false) }
    val view = LocalView.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (bubble.showSender) {
            val memberId = bubble.senderMemberId
            ProfileImage(
                fileName = bubble.senderImage,
                size = AVATAR_SIZE,
                modifier = if (memberId != null) {
                    Modifier.clickable { onOpenProfile(memberId) }
                } else {
                    Modifier
                },
            )
        } else {
            Spacer(modifier = Modifier.size(AVATAR_SIZE))
        }

        Column(horizontalAlignment = Alignment.Start) {
            if (bubble.showSender) {
                Text(
                    text = bubble.senderName ?: stringResource(R.string.chat_unknown_sender),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // 말풍선과 같은 둥근 모서리로 잘라야 길게 누를 때 리플이 각지지 않는다.
                var bubbleCenterY by remember { mutableStateOf(0f) }
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coords -> bubbleCenterY = coords.positionInWindow().y + coords.size.height / 2f }
                        .clip(RoundedCornerShape(BUBBLE_RADIUS))
                        .combinedClickable(
                            onClick = {
                                when (bubble.message.chatType) {
                                    ChatType.IMAGE -> onOpenImage(bubble.message.id)
                                    ChatType.FILE -> actions.onOpenOrDownload(bubble.message.message)
                                }
                            },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                barBelow = bubbleCenterY < view.rootView.height / 2f
                                showBar = true
                            },
                        ),
                ) {
                    if (showBar) {
                        ReactionBar(
                            current = bubble.myReaction,
                            below = barBelow,
                            onPick = { emoji ->
                                showBar = false
                                onReact(bubble.message.id, emoji)
                            },
                            onDismiss = { showBar = false },
                        )
                    }
                    BubbleBody(
                        bubble = bubble,
                        mine = false,
                        onOpenImage = onOpenImage,
                        attachments = attachments,
                        actions = actions,
                        handlesClick = false,
                    )
                }
                MetaColumn(bubble = bubble, alignment = Alignment.Start)
            }
            ReactionChips(
                reactions = bubble.message.reactions,
                myReaction = bubble.myReaction,
                onTap = { emoji -> onReact(bubble.message.id, emoji) },
                onShowReactors = { onShowReactors(bubble.message.id) },
            )
        }
    }
}

@Composable
private fun RightBubble(
    bubble: ChatBubble,
    onOpenImage: (Long) -> Unit,
    onResend: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onShowReactors: (Long) -> Unit,
    attachments: AttachmentUi,
    actions: AttachmentActions,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), horizontalAlignment = Alignment.End) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (bubble.isFailed) {
            FailedActions(
                onResend = { onResend(bubble.message.id) },
                onDelete = { onDelete(bubble.message.id) },
            )
        } else {
            MetaColumn(bubble = bubble, alignment = Alignment.End)
        }
        Spacer(modifier = Modifier.width(4.dp))
        BubbleBody(bubble = bubble, mine = true, onOpenImage = onOpenImage, attachments = attachments, actions = actions)
    }
    // 내 말풍선의 반응은 보기만 한다(내 메시지에는 남길 수 없다). 누르면 누가 남겼는지.
    ReactionChips(
        reactions = bubble.message.reactions,
        myReaction = null,
        onTap = null,
        onShowReactors = { onShowReactors(bubble.message.id) },
    )
    }
}

/** 실패한 말풍선은 시각·미읽음 대신 재전송/삭제와 `!` 를 보여 준다(부록 A §8a). */
@Composable
private fun FailedActions(onResend: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.chat_failed_mark),
            color = colorResource(R.color.red),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onResend, modifier = Modifier.size(ACTION_SIZE)) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.chat_resend),
                tint = colorResource(R.color.red),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(ACTION_SIZE)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.chat_delete),
                tint = colorResource(R.color.red),
            )
        }
    }
}

/** 시각과 미읽음 수. 미읽음은 0 이하면 감춘다. 실패 말풍선에는 둘 다 그리지 않는다. */
@Composable
private fun MetaColumn(bubble: ChatBubble, alignment: Alignment.Horizontal) {
    if (bubble.isFailed) return
    val unread = bubble.message.unreadCount
    if (unread <= 0 && !bubble.showTime) return

    Column(horizontalAlignment = alignment) {
        if (unread > 0) {
            Text(
                text = unread.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = colorResource(R.color.unread_marker),
            )
        }
        if (bubble.showTime) {
            Text(
                text = bubble.shortTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 본문. 이미지면 비율을 지킨 사진([ImageBubbleSize]), 아니면 글자다(파일·음성은 저장 파일 이름이 본문이다). */
/** [handlesClick] 가 false 면 사진을 눌러도 여기서 열지 않는다 — 바깥 상자가 탭·길게 누르기를 함께 받는다(상대 말풍선). */
@Composable
private fun BubbleBody(
    bubble: ChatBubble,
    mine: Boolean,
    onOpenImage: (Long) -> Unit,
    attachments: AttachmentUi,
    actions: AttachmentActions,
    handlesClick: Boolean = true,
) {
    when (bubble.message.chatType) {
        ChatType.FILE -> {
            FileBubble(
                fileName = bubble.message.message,
                info = attachments.fileInfos[bubble.message.message],
                download = attachments.fileDownloads[bubble.message.message] ?: FileDownloadUi(),
                mine = mine,
                onNeedInfo = actions.onNeedFileInfo,
                onClick = if (handlesClick) actions.onOpenOrDownload else null,
            )
            return
        }
        ChatType.AUDIO -> {
            AudioBubble(
                fileName = bubble.message.message,
                playback = attachments.audio?.takeIf { it.fileName == bubble.message.message },
                knownDurationMs = attachments.audioDurations[bubble.message.message],
                mine = mine,
                actions = actions,
            )
            return
        }
    }
    if (ChatViewModel.isImage(bubble.message)) {
        val fileName = bubble.message.message
        // 서버 메시지에는 사진 크기가 없다. 로드된 뒤 실제 크기로 상자를 잡고, 그 전에는 4:3 자리를 둔다.
        var boxSize by remember(fileName) { mutableStateOf(ImageBubbleSize.PLACEHOLDER) }
        val density = LocalDensity.current
        // 상자보다 작은 캐시 비트맵을 늘려 쓰지 않도록 최대 상자 크기로 정확히 디코딩해 둔다.
        // 어떤 비율이든 말풍선의 긴 변은 이 상자를 넘지 않으므로 이 한 장으로 충분하다.
        val requestSize = with(density) {
            Size(ImageBubbleSize.MAX_WIDTH.roundToPx(), ImageBubbleSize.MAX_HEIGHT.roundToPx())
        }
        // 옛 앱처럼 사진을 말풍선 색 바탕 안에 여백을 두고 넣는다 — 그 바탕이 테두리가 된다(내 사진은 내 말풍선 색).
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BUBBLE_RADIUS))
                .background(if (mine) colorResource(R.color.chat_bubble) else MaterialTheme.colorScheme.surfaceVariant)
                .then(if (handlesClick) Modifier.clickable { onOpenImage(bubble.message.id) } else Modifier)
                .padding(IMAGE_FRAME),
        ) {
            ChatImage(
                fileName = fileName,
                modifier = Modifier
                    .size(boxSize)
                    .clip(RoundedCornerShape(BUBBLE_RADIUS - IMAGE_FRAME)),
                precision = Precision.EXACT,
                requestSize = requestSize,
                onLoaded = { width, height -> boxSize = ImageBubbleSize.of(width, height) },
            )
        }
        return
    }

    // 기존 앱 말풍선의 maxWidth="250sp" 와 같게 글자 크기 배율을 따라 넓어진다.
    val maxBubbleWidth = with(LocalDensity.current) { TEXT_MAX_WIDTH_SP.toDp() }
    Text(
        text = bubble.message.message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (mine) colorResource(R.color.on_chat_bubble) else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .widthIn(max = maxBubbleWidth)
            .clip(RoundedCornerShape(BUBBLE_RADIUS))
            .background(
                if (mine) colorResource(R.color.chat_bubble) else MaterialTheme.colorScheme.surfaceVariant,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** 말풍선 바탕색. 내 것은 보라, 상대는 회색. */
@Composable
private fun bubbleContainer(mine: Boolean): Color =
    if (mine) colorResource(R.color.chat_bubble) else MaterialTheme.colorScheme.surfaceVariant

@Composable
private fun bubbleContent(mine: Boolean): Color =
    if (mine) colorResource(R.color.on_chat_bubble) else MaterialTheme.colorScheme.onSurface

/**
 * 파일 말풍선. 아이콘 + 원본 이름 + 크기(또는 상태). 원본 이름은 storage 의 file-info 에서 받는다(오기 전엔 저장 이름).
 * 안 받았으면 누를 때 Downloads 에 저장하고(받는 동안 스피너), 받은 뒤에는 "저장됨 · 눌러서 열기".
 * 상대 말풍선은 바깥 상자가 탭을 받으므로 [onClick] 이 null 이다.
 */
@Composable
private fun FileBubble(
    fileName: String,
    info: FileInfo?,
    download: FileDownloadUi,
    mine: Boolean,
    onNeedInfo: (String) -> Unit,
    onClick: ((String) -> Unit)?,
) {
    LaunchedEffect(fileName) { onNeedInfo(fileName) }
    val content = bubbleContent(mine)
    val accent = colorResource(R.color.brand_violet)
    val sizeText = info?.takeIf { it.sizeBytes >= 0 }?.let { FileSizeText.of(it.sizeBytes) }
    val statusText = when (download.state) {
        FileDownloadUi.State.DOWNLOADING -> stringResource(R.string.chat_file_downloading)
        FileDownloadUi.State.DONE -> stringResource(R.string.chat_file_saved_hint)
        FileDownloadUi.State.IDLE -> stringResource(R.string.chat_file_download_hint)
    }
    Row(
        modifier = Modifier
            .widthIn(min = 180.dp, max = ATTACH_MAX_WIDTH)
            .clip(RoundedCornerShape(BUBBLE_RADIUS))
            .background(bubbleContainer(mine))
            .then(if (onClick != null) Modifier.clickable { onClick(fileName) } else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 왼쪽 동그라미: 파일 아이콘 / 받는 중 스피너 / 받음 체크. 음성의 재생 버튼과 같은 크기.
        Box(
            modifier = Modifier.size(ATTACH_CIRCLE).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            when (download.state) {
                FileDownloadUi.State.DOWNLOADING -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                FileDownloadUi.State.DONE -> Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.chat_file_saved_hint),
                    tint = Color.White,
                    modifier = Modifier.size(ATTACH_ICON),
                )
                FileDownloadUi.State.IDLE -> Icon(
                    Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = stringResource(R.string.chat_file_icon),
                    tint = Color.White,
                    modifier = Modifier.size(ATTACH_ICON),
                )
            }
        }
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = info?.originalName ?: fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(sizeText, statusText).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 음성 말풍선 = 재생기. 왼쪽 동그란 재생/일시정지 버튼, 가운데 진행 막대(탭·드래그로 이동),
 * 아래 현재 시각과 남은 시각, 오른쪽 음소거. [playback] 이 null 이면 이 파일은 재생기에 올라 있지 않은 것이라
 * 처음 상태로 그리되, 미리 읽어 둔 [knownDurationMs] 가 있으면 총 길이를 보여 준다.
 */
@Composable
private fun AudioBubble(
    fileName: String,
    playback: AudioPlayback?,
    knownDurationMs: Long?,
    mine: Boolean,
    actions: AttachmentActions,
) {
    LaunchedEffect(fileName) { actions.onNeedAudioDuration(fileName) }
    val content = bubbleContent(mine)
    val accent = colorResource(R.color.brand_violet)
    val duration = playback?.durationMs?.takeIf { it > 0L } ?: knownDurationMs ?: 0L
    val loaded = playback != null && !playback.isLoading && !playback.failed
    // 막대를 끄는 동안은 손가락 위치를, 놓으면 재생 위치를 따라간다.
    var dragging by remember(fileName) { mutableStateOf(false) }
    var dragValue by remember(fileName) { mutableStateOf(0f) }
    val progress = if (dragging) dragValue else (playback?.progress ?: 0f)
    val shownPosition = if (dragging) (dragValue * duration).toLong() else (playback?.positionMs ?: 0L)

    Row(
        modifier = Modifier
            .width(AUDIO_WIDTH)
            .clip(RoundedCornerShape(BUBBLE_RADIUS))
            .background(bubbleContainer(mine))
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(ATTACH_CIRCLE)
                .clip(CircleShape)
                .background(accent)
                .clickable { actions.onToggleAudio(fileName) },
            contentAlignment = Alignment.Center,
        ) {
            when {
                playback?.isLoading == true -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                playback?.isPlaying == true -> Icon(
                    Icons.Filled.Pause,
                    contentDescription = stringResource(R.string.chat_audio_pause),
                    tint = Color.White,
                    modifier = Modifier.size(ATTACH_ICON),
                )
                else -> Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.chat_audio_play),
                    tint = Color.White,
                    modifier = Modifier.size(ATTACH_ICON),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            AudioProgressBar(
                progress = progress,
                enabled = loaded && duration > 0L,
                active = accent,
                inactive = content.copy(alpha = 0.2f),
                onScrub = { value ->
                    dragging = true
                    dragValue = value
                },
                onScrubEnd = { value ->
                    dragging = false
                    actions.onSeekAudio((value * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (playback != null) AudioTime.of(shownPosition) else stringResource(R.string.chat_audio_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (playback?.isPlaying == true) FontWeight.SemiBold else FontWeight.Normal,
                    color = content,
                )
                Text(
                    text = when {
                        playback?.failed == true -> stringResource(R.string.chat_audio_failed)
                        playback != null && duration > 0L -> "-" + AudioTime.of(duration - shownPosition)
                        duration > 0L -> AudioTime.of(duration)
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (playback?.failed == true) colorResource(R.color.red) else content.copy(alpha = 0.7f),
                )
            }
        }
        // 재생 버튼과 같은 크기의 동그라미 안에 같은 크기 아이콘. 음소거면 빨간색.
        IconButton(onClick = actions.onToggleMute, modifier = Modifier.size(ATTACH_CIRCLE)) {
            val muted = playback?.isMuted == true
            Icon(
                if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(if (muted) R.string.chat_audio_unmute else R.string.chat_audio_mute),
                tint = if (muted) colorResource(R.color.red) else content.copy(alpha = 0.8f),
                modifier = Modifier.size(ATTACH_ICON),
            )
        }
    }
}

/**
 * 재생 진행 막대. 가는 트랙 위에 진행분과 동그란 손잡이를 그린다. 탭하면 그 자리로, 끌면 따라간다.
 * Material Slider 는 손잡이·트랙이 두꺼워 말풍선 안에서 둔해 보여 직접 그린다.
 */
@Composable
private fun AudioProgressBar(
    progress: Float,
    enabled: Boolean,
    active: Color,
    inactive: Color,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var widthPx by remember { mutableStateOf(1f) }
    fun fraction(x: Float): Float = (x / widthPx).coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .height(PROGRESS_TOUCH_HEIGHT)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val value = fraction(offset.x)
                    onScrub(value)
                    onScrubEnd(value)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                var value = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        value = fraction(offset.x)
                        onScrub(value)
                    },
                    onDragEnd = { onScrubEnd(value) },
                    onDragCancel = { onScrubEnd(value) },
                ) { change, _ ->
                    value = fraction(change.position.x)
                    onScrub(value)
                    change.consume()
                }
            }
            .semantics {
                contentDescription = ""
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
    ) {
        val trackHeight = PROGRESS_TRACK_HEIGHT.toPx()
        val centerY = size.height / 2f
        val radius = CornerRadius(trackHeight / 2f)
        drawRoundRect(
            color = inactive,
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
            cornerRadius = radius,
        )
        val x = size.width * progress.coerceIn(0f, 1f)
        if (x > 0f) {
            drawRoundRect(
                color = active,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = androidx.compose.ui.geometry.Size(x, trackHeight),
                cornerRadius = radius,
            )
        }
        if (enabled) {
            drawCircle(color = active, radius = PROGRESS_THUMB_RADIUS.toPx(), center = Offset(x, centerY))
        }
    }
}

/**
 * 채팅 사진. [ProfileImage] 와 같은 주소 규칙을 쓰되 동그랗게 자르지 않는다.
 *
 * 말풍선·드로어 미리보기는 기본값(잘라서 채움, 크기 대충 맞음)으로 충분하다. 전체 보기는
 * [contentScale] 을 [ContentScale.Fit] 으로, [precision] 을 [Precision.EXACT] 로 준다 —
 * Coil 은 크기가 대충 맞아도 되는 요청이면 말풍선용으로 작게 디코딩해 둔 비트맵을 그대로
 * 돌려주므로, 화면 크기로 늘리면 뭉개진다. EXACT 면 화면 크기에 맞춰 다시 디코딩한다.
 */
@Composable
fun ChatImage(
    fileName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    precision: Precision = Precision.INEXACT,
    /** 디코딩할 크기. 없으면 Coil 이 그려지는 크기로 정한다. 말풍선처럼 그려지는 크기가 나중에 정해질 때 준다. */
    requestSize: Size? = null,
    /** 사진이 로드되면 실제 픽셀 크기(가로, 세로)를 알려 준다. 말풍선이 비율을 잡는 데 쓴다. */
    onLoaded: ((width: Int, height: Int) -> Unit)? = null,
) {
    val placeholder = painterResource(R.drawable.basic_profile_image)
    val context = LocalContext.current
    val url = fileName.takeIf { it.isNotBlank() }?.let { ApiConfig.imageUrl(it) }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .precision(precision)
            .apply { if (requestSize != null) size(requestSize) }
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        onSuccess = { state ->
            val drawable = state.result.drawable
            onLoaded?.invoke(drawable.intrinsicWidth, drawable.intrinsicHeight)
        },
    )
}

private val AVATAR_SIZE = 40.dp
private val ACTION_SIZE = 32.dp
/** 파일 말풍선 최대 폭. */
private val ATTACH_MAX_WIDTH = 260.dp
/** 음성 말풍선(재생기) 폭. */
private val AUDIO_WIDTH = 250.dp
private val PROGRESS_TOUCH_HEIGHT = 18.dp
/** 파일·음성 말풍선의 동그란 버튼과 그 안 아이콘 크기. 재생·파일·음소거가 모두 같다. */
private val ATTACH_CIRCLE = 36.dp
private val ATTACH_ICON = 20.dp
private val PROGRESS_TRACK_HEIGHT = 4.dp
private val PROGRESS_THUMB_RADIUS = 6.dp
private val BUBBLE_RADIUS = 12.dp
/** 사진 둘레의 말풍선 색 테두리 두께. */
private val IMAGE_FRAME = 4.dp
private val TEXT_MAX_WIDTH_SP = 250.sp
