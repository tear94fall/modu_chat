package com.example.modumessenger.feature.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DrawerDefaults
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.modumessenger.core.ui.components.CircleIconButton
import com.example.modumessenger.core.ui.components.StatusBarBand
import com.example.modumessenger.core.ui.theme.BrandViolet
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.modumessenger.R
import com.example.modumessenger.core.model.ReactionEmoji
import com.example.modumessenger.core.model.Member
import com.example.modumessenger.core.ui.components.ConfirmDialog
import com.example.modumessenger.core.ui.components.ModuTopBar
import com.example.modumessenger.core.ui.components.ProfileImage
import com.example.modumessenger.core.ui.components.rememberComingSoon
import com.example.modumessenger.core.ui.components.rememberSnackbarThen
import com.example.modumessenger.core.util.DisplayName
import kotlinx.coroutines.launch

/**
 * 채팅방(부록 A §8). 오른쪽 드로어는 Material3 가 왼쪽만 지원하므로 전체를 RTL 로 뒤집고
 * 내용만 다시 LTR 로 되돌려 오른쪽에서 열리게 한다.
 */
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onOpenProfile: (Long) -> Unit,
    onOpenRoomEdit: (String) -> Unit,
    onOpenInvite: (String, List<String>) -> Unit,
    onOpenImages: (List<String>) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val names by viewModel.names.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val comingSoon = rememberComingSoon(snackbarHostState)
    val announce = rememberSnackbarThen(snackbarHostState)
    val roomSettingsLabel = stringResource(R.string.chat_drawer_settings_moving)

    var showAttachSheet by remember { mutableStateOf(false) }
    var resendTarget by remember { mutableStateOf<Long?>(null) }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }
    var reactorsTarget by remember { mutableStateOf<Long?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    var atBottom by remember { mutableStateOf(true) }
    var pendingScrollToBottom by remember { mutableStateOf(false) }
    var firstScrollDone by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(message.res, *message.args.toTypedArray()))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.leftRoom.collect { onBack() }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // 맨 아래에 붙어 있는지 알려 준다. 붙으면 "아래로" 배지가 사라진다.
    LaunchedEffect(listState) {
        snapshotFlow { !listState.canScrollForward }.collect { bottom ->
            atBottom = bottom
            viewModel.onAtBottomChanged(bottom)
        }
    }

    // 맨 위에 닿으면 이전 메시지를 붙이고, 보이던 자리를 그대로 유지한다.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.layoutInfo.totalItemsCount }
            .collect { (first, total) ->
                if (first != 0 || total < ChatViewModel.PAGE_SIZE) return@collect
                val offset = listState.firstVisibleItemScrollOffset
                val added = viewModel.loadPrev()
                if (added > 0) listState.scrollToItem(added, offset)
            }
    }

    // 새 메시지(목록의 마지막 id 가 바뀐 것)일 때만 아래로 따라간다. 위로 더 불러온 것은 움직이지 않는다.
    val lastBubbleId = uiState.bubbles.lastOrNull()?.message?.id
    LaunchedEffect(lastBubbleId) {
        val size = uiState.bubbles.size
        if (lastBubbleId == null || size == 0) return@LaunchedEffect
        if (!firstScrollDone) {
            firstScrollDone = true
            listState.scrollToItem(size - 1)
            return@LaunchedEffect
        }
        if (pendingScrollToBottom || atBottom) {
            pendingScrollToBottom = false
            listState.animateScrollToItem(size - 1)
        }
    }

    // 키보드가 올라오는 동안 목록을 맨 아래에 붙인다(기존 앱 adjustResize + scrollToPosition 과 같은 동작).
    // 인셋이 프레임마다 커지므로 매번 마지막 항목으로 맞추고, 내려갈 때는 손대지 않는다
    // (키보드를 닫을 때 위로 올려 둔 위치가 튀지 않게).
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    var lastImeBottom by remember { mutableIntStateOf(imeBottom) }
    LaunchedEffect(imeBottom) {
        val opening = imeBottom > lastImeBottom
        lastImeBottom = imeBottom
        if (!opening) return@LaunchedEffect
        val size = uiState.bubbles.size
        if (size > 0) listState.scrollToItem(size - 1)
        viewModel.onAtBottomChanged(true)
    }

    val scrollToBottom: () -> Unit = {
        scope.launch {
            val size = uiState.bubbles.size
            if (size > 0) listState.animateScrollToItem(size - 1)
            viewModel.onAtBottomChanged(true)
        }
        Unit
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    // 상태 바가 투명이라 흰 시트가 그 뒤까지 차오르면 흰 시계가 안 보인다.
                    // 시트는 위 인셋을 직접 처리하지 않고, 그 자리에 상단바와 같은 보라 띠를 깐다.
                    ModalDrawerSheet(
                        windowInsets = DrawerDefaults.windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    ) {
                        StatusBarBand()
                        ChatDrawerContent(
                            uiState = uiState,
                            names = names,
                            onOpenProfile = onOpenProfile,
                            onOpenRoomEdit = {
                                announce(roomSettingsLabel) { onOpenRoomEdit(viewModel.roomId) }
                            },
                            onOpenPhotos = { viewModel.recentImageIds()?.let(onOpenImages) },
                            onOpenPhotoAt = { index ->
                                viewModel.recentImageIds()?.let { ids -> onOpenImages(ids.drop(index)) }
                            },
                            onMembersHeader = comingSoon,
                            onExit = { showExitDialog = true },
                            onInvite = { onOpenInvite(viewModel.roomId, uiState.memberUserIds) },
                        )
                    }
                }
            },
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    topBar = {
                        ModuTopBar(title = uiState.title, onBack = onBack) {
                            IconButton(onClick = comingSoon) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.chat_menu_search),
                                )
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = stringResource(R.string.chat_menu_drawer),
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { padding ->
                    // Scaffold 패딩(상단바 + 내비게이션 바)을 쓴 만큼 소비해야 imePadding 이 같은 인셋을 다시 더하지 않는다.
                    Column(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                            ) {
                                items(uiState.bubbles, key = { it.message.id }) { bubble ->
                                    ChatBubbleRow(
                                        bubble = bubble,
                                        onOpenProfile = onOpenProfile,
                                        onOpenImage = { id -> onOpenImages(listOf(id.toString())) },
                                        onResend = { id -> resendTarget = id },
                                        onDelete = { id -> deleteTarget = id },
                                        onReact = viewModel::react,
                                        onShowReactors = { id -> reactorsTarget = id },
                                    )
                                }
                            }
                            if (uiState.jumpToBottomCount > 0) {
                                JumpToBottomBadge(
                                    count = uiState.jumpToBottomCount,
                                    onClick = scrollToBottom,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                                )
                            }
                        }
                        // 기존 앱 입력줄 위의 1dp 구분선.
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = colorResource(R.color.divider),
                        )
                        ChatInputRow(
                            value = uiState.input,
                            onValueChange = viewModel::onInputChange,
                            onAttach = { showAttachSheet = true },
                            onSend = {
                                if (uiState.input.isNotEmpty()) {
                                    pendingScrollToBottom = true
                                    viewModel.send()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAttachSheet) {
        AttachSheet(
            snackbarHostState = snackbarHostState,
            onDismiss = { showAttachSheet = false },
            onPicked = { uri, kind ->
                pendingScrollToBottom = true
                viewModel.onAttachmentPicked(uri, kind)
            },
            takePictureUri = viewModel::takePictureUri,
        )
    }

    resendTarget?.let { id ->
        ConfirmDialog(
            title = null,
            text = stringResource(R.string.chat_resend_message),
            confirmText = stringResource(R.string.chat_resend),
            dismissText = stringResource(R.string.chat_cancel),
            onConfirm = {
                resendTarget = null
                viewModel.resend(id)
            },
            onDismiss = { resendTarget = null },
        )
    }

    reactorsTarget?.let { id ->
        val bubble = uiState.bubbles.firstOrNull { it.message.id == id }
        if (bubble == null || bubble.message.reactions.isEmpty()) {
            reactorsTarget = null
        } else {
            val names by viewModel.names.collectAsStateWithLifecycle()
            val unknown = stringResource(R.string.chat_unknown_sender)
            val byUserId = uiState.room?.members.orEmpty().associateBy { it.userId }
            val lines = bubble.message.reactions.joinToString("\n") { reaction ->
                val who = reaction.userIds.joinToString(", ") { userId ->
                    val member = byUserId[userId]
                    DisplayName.of(userId, member?.username, names).ifBlank { unknown }
                }
                "${ReactionEmoji.text(reaction.emoji)} $who"
            }
            AlertDialog(
                onDismissRequest = { reactorsTarget = null },
                title = { Text(stringResource(R.string.chat_reactions_title)) },
                text = { Text(lines) },
                confirmButton = { TextButton(onClick = { reactorsTarget = null }) { Text(stringResource(R.string.chat_ok)) } },
            )
        }
    }

    deleteTarget?.let { id ->
        ConfirmDialog(
            title = null,
            text = stringResource(R.string.chat_delete_message),
            confirmText = stringResource(R.string.chat_delete),
            dismissText = stringResource(R.string.chat_cancel),
            onConfirm = {
                deleteTarget = null
                viewModel.deleteFailed(id)
            },
            onDismiss = { deleteTarget = null },
        )
    }

    if (showExitDialog) {
        ConfirmDialog(
            title = stringResource(R.string.chat_exit_title),
            text = stringResource(R.string.chat_exit_message),
            confirmText = stringResource(R.string.chat_drawer_exit),
            dismissText = stringResource(R.string.chat_cancel),
            confirmColor = colorResource(R.color.red),
            onConfirm = {
                showExitDialog = false
                viewModel.leaveRoom()
            },
            onDismiss = { showExitDialog = false },
        )
    }
}

/** 남이 보낸 새 메시지가 쌓여 있음을 알린다. 누르면 맨 아래로 간다. */
@Composable
private fun JumpToBottomBadge(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = stringResource(R.string.chat_jump_to_bottom, count),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** 입력줄: `+`(첨부) · 입력칸 · 전송. */
@Composable
private fun ChatInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit,
) {
    // 입력칸은 말풍선 한 줄(14sp 글자 + 위아래 8dp = 36dp)과 같은 높이. 버튼도 같은 36dp 원이다.
    // 줄 전체는 구분선 1dp + 여백 5dp + 36dp + 여백 6dp = 48dp. 입력칸이 여러 줄이면 그만큼만 늘어난다.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // `+` 도 보내기처럼 원 바탕을 깐다. 주된 동작(보내기)과 구분되게 옅은 보라 원에 보라 아이콘.
        CircleIconButton(
            onClick = onAttach,
            icon = Icons.Filled.Add,
            contentDescription = stringResource(R.string.chat_attach),
            size = INPUT_BUTTON_SIZE,
            iconSize = 22.dp,
            background = BrandViolet.copy(alpha = 0.15f),
            tint = BrandViolet,
        )
        // 말풍선과 같은 글자 크기(bodyMedium)·여백(8dp)이라 한 줄일 때 높이가 말풍선과 같다(최대 4줄).
        // Material3 TextField 는 최소 56dp 라 세로로 너무 길다.
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            maxLines = 4,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = INPUT_MIN_HEIGHT)
                        .clip(RoundedCornerShape(INPUT_RADIUS))
                        .background(colorResource(R.color.chat_input_fill))
                        .border(1.dp, colorResource(R.color.chat_input_stroke), RoundedCornerShape(INPUT_RADIUS))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.chat_input_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorResource(R.color.grey),
                        )
                    }
                    inner()
                }
            },
        )
        // 보내기는 보라 원에 흰 아이콘(`bg_send_button`).
        CircleIconButton(
            onClick = onSend,
            icon = Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(R.string.chat_send),
            size = INPUT_BUTTON_SIZE,
            iconSize = 18.dp,
            background = BrandViolet,
            tint = Color.White,
        )
    }
}

/** 오른쪽 드로어 안쪽(부록 A §8 · §8b). */
@Composable
private fun ChatDrawerContent(
    uiState: ChatUiState,
    names: Map<String, String>,
    onOpenProfile: (Long) -> Unit,
    onOpenRoomEdit: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenPhotoAt: (Int) -> Unit,
    onMembersHeader: () -> Unit,
    onExit: () -> Unit,
    onInvite: () -> Unit,
) {
    val room = uiState.room
    val previewImages = uiState.recentImages.take(DRAWER_IMAGE_COUNT)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileImage(fileName = room?.roomImage, size = 56.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = room?.roomName.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.chat_drawer_member_count, uiState.memberCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenRoomEdit) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.chat_drawer_settings),
                        )
                    }
                }
                HorizontalDivider()
                DrawerSectionRow(text = stringResource(R.string.chat_drawer_photos), onClick = onOpenPhotos)
                if (previewImages.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        previewImages.forEachIndexed { index, message ->
                            ChatImage(
                                fileName = message.message,
                                modifier = Modifier
                                    .weight(1f)
                                    .size(DRAWER_IMAGE_SIZE)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onOpenPhotoAt(index) },
                            )
                        }
                        repeat(DRAWER_IMAGE_COUNT - previewImages.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                    HorizontalDivider()
                }
                DrawerSectionRow(
                    text = stringResource(R.string.chat_drawer_members),
                    onClick = onMembersHeader,
                )
            }

            items(room?.members.orEmpty(), key = { it.id }) { member ->
                DrawerMemberRow(
                    member = member,
                    names = names,
                    onClick = { if (member.id > 0L) onOpenProfile(member.id) },
                )
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.chat_drawer_exit))
            }
            OutlinedButton(onClick = onInvite, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.chat_drawer_invite))
            }
        }
    }
}

@Composable
private fun DrawerSectionRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** 드로어 멤버 한 줄(부록 A §8b). 상태 메시지는 15자를 넘으면 12자 + `"..."`. */
@Composable
private fun DrawerMemberRow(member: Member, names: Map<String, String>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProfileImage(fileName = member.profileImage, size = 40.dp, modifier = Modifier.clip(CircleShape))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = DisplayName.of(member.userId, member.username, names),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val status = shortenStatus(member.statusMessage)
            if (status.isNotEmpty()) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 15자를 넘으면 12자 + `"..."`(부록 A §8b). */
internal fun shortenStatus(statusMessage: String): String =
    if (statusMessage.length > 15) statusMessage.take(12) + "..." else statusMessage

private const val DRAWER_IMAGE_COUNT = 3
private val DRAWER_IMAGE_SIZE = 96.dp

private val INPUT_MIN_HEIGHT = 36.dp
private val INPUT_BUTTON_SIZE = 36.dp
private val INPUT_RADIUS = 18.dp
