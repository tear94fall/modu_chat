package com.example.modumessenger.feature.chat

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.modumessenger.core.model.Reaction
import com.example.modumessenger.core.model.ReactionEmoji

private val BAR_HEIGHT = 38.dp
private val BAR_GAP = 4.dp
private val EMOJI_CELL = 30.dp
private val EMOJI_SIZE = 17.sp

/**
 * 이모지 6개 바. 말풍선이 화면 위쪽 절반에 있으면 말풍선 아래에, 아래쪽 절반에 있으면 위에 띄운다([below]).
 * 부모 Box 의 왼쪽 모서리에 붙이고 바 높이만큼 밀어낸다. 바깥을 누르거나 뒤로 가면 닫힌다.
 * 내가 이미 남긴 이모지는 동그란 배경으로 표시한다.
 */
@Composable
fun ReactionBar(
    current: String?,
    below: Boolean,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val distance = with(LocalDensity.current) { (BAR_HEIGHT + BAR_GAP).roundToPx() }
    Popup(
        alignment = if (below) Alignment.BottomStart else Alignment.TopStart,
        offset = IntOffset(0, if (below) distance else -distance),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(19.dp),
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReactionEmoji.ALL.forEach { key ->
                    val selected = key == current
                    Text(
                        text = ReactionEmoji.text(key),
                        fontSize = EMOJI_SIZE,
                        modifier = Modifier
                            .size(EMOJI_CELL)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { onPick(key) }
                            .padding(top = 3.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * 말풍선 아래 반응 칩(이모지 + 수). 내가 남긴 칩은 테두리로 강조.
 * [onTap] 이 null 이면(내 말풍선) 탭으로 토글할 수 없고, 누르면 누른 사람 목록만 연다.
 */
@Composable
fun ReactionChips(
    reactions: List<Reaction>,
    myReaction: String?,
    onTap: ((String) -> Unit)?,
    onShowReactors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reactions.isEmpty()) return
    val view = LocalView.current
    Row(modifier = modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        reactions.forEach { reaction ->
            val mine = reaction.emoji == myReaction
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = if (mine) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.reactionChipClickable(
                    onClick = {
                        if (onTap != null) onTap(reaction.emoji) else onShowReactors()
                    },
                    onLongClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onShowReactors()
                    },
                ),
            ) {
                Text(
                    text = "${ReactionEmoji.text(reaction.emoji)} ${reaction.count}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.reactionChipClickable(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    combinedClickable(onClick = onClick, onLongClick = onLongClick)
