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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
) {
    if (bubble.isMine) {
        RightBubble(bubble, onOpenImage, onResend, onDelete, onShowReactors, modifier)
    } else {
        LeftBubble(bubble, onOpenProfile, onOpenImage, onReact, onShowReactors, modifier)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LeftBubble(
    bubble: ChatBubble,
    onOpenProfile: (Long) -> Unit,
    onOpenImage: (Long) -> Unit,
    onReact: (Long, String) -> Unit,
    onShowReactors: (Long) -> Unit,
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
                            onClick = { if (ChatViewModel.isImage(bubble.message)) onOpenImage(bubble.message.id) },
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
                    BubbleBody(bubble = bubble, mine = false, onOpenImage = onOpenImage, handlesClick = false)
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
        BubbleBody(bubble = bubble, mine = true, onOpenImage = onOpenImage)
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

/** 본문. 이미지면 200dp 폭 사진, 아니면 글자다(파일·음성은 저장 파일 이름이 본문이다). */
/** [handlesClick] 가 false 면 사진을 눌러도 여기서 열지 않는다 — 바깥 상자가 탭·길게 누르기를 함께 받는다(상대 말풍선). */
@Composable
private fun BubbleBody(bubble: ChatBubble, mine: Boolean, onOpenImage: (Long) -> Unit, handlesClick: Boolean = true) {
    if (ChatViewModel.isImage(bubble.message)) {
        // 옛 앱처럼 사진을 말풍선 색 바탕 안에 여백을 두고 넣는다 — 그 바탕이 테두리가 된다(내 사진은 내 말풍선 색).
        // 세로로 긴 사진(스크린샷 등)이 화면을 다 덮지 않게 높이를 제한한다. 전체는 눌러서 뷰어로 본다.
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(BUBBLE_RADIUS))
                .background(if (mine) colorResource(R.color.chat_bubble) else MaterialTheme.colorScheme.surfaceVariant)
                .then(if (handlesClick) Modifier.clickable { onOpenImage(bubble.message.id) } else Modifier)
                .padding(IMAGE_FRAME),
        ) {
            ChatImage(
                fileName = bubble.message.message,
                modifier = Modifier
                    .width(IMAGE_WIDTH)
                    .heightIn(min = IMAGE_MIN_HEIGHT, max = IMAGE_MAX_HEIGHT)
                    .clip(RoundedCornerShape(BUBBLE_RADIUS - IMAGE_FRAME)),
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

/** 채팅 사진. [ProfileImage] 와 같은 주소 규칙을 쓰되 동그랗게 자르지 않는다. */
@Composable
fun ChatImage(fileName: String, modifier: Modifier = Modifier) {
    val placeholder = painterResource(R.drawable.basic_profile_image)
    AsyncImage(
        model = fileName.takeIf { it.isNotBlank() }?.let { ApiConfig.imageUrl(it) },
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
    )
}

private val AVATAR_SIZE = 40.dp
private val IMAGE_WIDTH = 200.dp
private val IMAGE_MIN_HEIGHT = 120.dp
private val IMAGE_MAX_HEIGHT = 260.dp
private val ACTION_SIZE = 32.dp
private val BUBBLE_RADIUS = 12.dp
/** 사진 둘레의 말풍선 색 테두리 두께. */
private val IMAGE_FRAME = 4.dp
private val TEXT_MAX_WIDTH_SP = 250.sp
