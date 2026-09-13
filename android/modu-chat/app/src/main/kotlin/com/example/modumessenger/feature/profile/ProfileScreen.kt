package com.example.modumessenger.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.modumessenger.R
import com.example.modumessenger.core.model.ProfileType
import com.example.modumessenger.core.network.ApiConfig
import com.example.modumessenger.core.ui.components.ConfirmDialog
import com.example.modumessenger.core.ui.components.ErrorBox
import com.example.modumessenger.core.ui.components.LoadingBox
import com.example.modumessenger.core.ui.components.ProfileImage
import com.example.modumessenger.core.ui.components.swipeDownToClose
import com.example.modumessenger.core.ui.theme.BrandViolet
import com.example.modumessenger.core.ui.theme.ModuRed
import com.example.modumessenger.core.ui.theme.ModuGrey
import com.example.modumessenger.core.ui.theme.OnChatBubble
import com.example.modumessenger.core.ui.theme.ProfileSurface
import com.example.modumessenger.core.util.DisplayName

private val AvatarSize = 112.dp
private val ScrimHeight = 96.dp

/**
 * 프로필 보기(부록 A §13).
 *
 * 배경은 화면 폭과 같은 1:1 정사각형이고, 프로필 사진은 그 아래 선에 반씩 걸친다.
 */
@Composable
fun ProfileScreen(
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onHistory: (Long) -> Unit,
    onOpenImage: (Long, String) -> Unit,
    onOpenRoom: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val names by viewModel.names.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(message.res, *message.args.toTypedArray()))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.OpenRoom -> onOpenRoom(event.roomId)
            }
        }
    }

    Scaffold(
        containerColor = ProfileSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val member = uiState.member
            when {
                member == null && uiState.isLoading -> LoadingBox()
                member == null && uiState.failed ->
                    ErrorBox(
                        message = stringResource(R.string.profile_connect_failed),
                        onRetry = viewModel::onResume,
                    )

                member != null -> ProfileContent(
                    onClose = onClose,
                    name = DisplayName.of(member.userId, member.username, names),
                    statusMessage = member.statusMessage,
                    profileImage = member.profileImage,
                    wallpaperImage = member.wallpaperImage,
                    uiState = uiState,
                    onEdit = onEdit,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onStartChat = viewModel::startChat,
                    onRename = viewModel::showRenameDialog,
                    onWallpaperClick = {
                        onOpenImage(viewModel.memberId, ProfileType.PROFILE_WALLPAPER.name)
                    },
                    onAvatarClick = {
                        onOpenImage(viewModel.memberId, ProfileType.PROFILE_IMAGE.name)
                    },
                )
            }

            // 기록·닫기 아이콘은 배경사진 위에 떠 있다(스크림 덕분에 밝은 사진에서도 읽힌다).
            if (uiState.showHistoryButton) {
                ScrimIconButton(
                    onClick = { onHistory(viewModel.memberId) },
                    icon = Icons.Filled.Image,
                    contentDescription = stringResource(R.string.profile_history_button),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                )
            }
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(TOP_BUTTON_GAP),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (uiState.showFriendActions) {
                    FriendMenuButton(
                        isHidden = uiState.isHidden,
                        isBlocked = uiState.isBlocked,
                        enabled = !uiState.updatingFlag,
                        onHide = viewModel::showHideDialog,
                        onUnhide = { viewModel.setHidden(false) },
                        onBlock = viewModel::showBlockDialog,
                        onUnblock = { viewModel.setBlocked(false) },
                    )
                }
                ScrimIconButton(
                    onClick = onClose,
                    icon = Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.profile_close_button),
                )
            }
        }
    }

    if (uiState.hideDialogVisible) {
        ConfirmDialog(
            title = stringResource(R.string.profile_hide_confirm_title),
            text = stringResource(R.string.profile_hide_confirm_text),
            confirmText = stringResource(R.string.profile_hide_confirm_ok),
            dismissText = stringResource(R.string.profile_hide_confirm_cancel),
            onConfirm = { viewModel.setHidden(true) },
            onDismiss = viewModel::dismissHideDialog,
        )
    }

    if (uiState.blockDialogVisible) {
        ConfirmDialog(
            title = stringResource(R.string.profile_block_confirm_title),
            text = stringResource(R.string.profile_block_confirm_text),
            confirmText = stringResource(R.string.profile_block_confirm_ok),
            dismissText = stringResource(R.string.profile_block_confirm_cancel),
            onConfirm = { viewModel.setBlocked(true) },
            onDismiss = viewModel::dismissBlockDialog,
            confirmColor = ModuRed,
        )
    }

    if (uiState.renameDialogVisible) {
        RenameFriendDialog(
            value = uiState.renameInput,
            canSave = uiState.canSaveRename,
            onValueChange = viewModel::onRenameInputChange,
            onSave = viewModel::saveRename,
            onDismiss = viewModel::dismissRenameDialog,
        )
    }
}

@Composable
private fun ProfileContent(
    onClose: () -> Unit,
    name: String,
    statusMessage: String,
    profileImage: String,
    wallpaperImage: String,
    uiState: ProfileUiState,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStartChat: () -> Unit,
    onRename: () -> Unit,
    onWallpaperClick: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Wallpaper(fileName = wallpaperImage, onClose = onClose, onClick = onWallpaperClick)
            // 배경사진 아래 선에 반씩 걸친다(아래로 절반만큼 내린다).
            Box(
                modifier = Modifier.offset(y = AvatarSize / 2),
                contentAlignment = Alignment.Center,
            ) {
                // 흰 테두리 4dp 는 바깥 배경 + 안쪽 여백으로 만든다(전체 112dp).
                ProfileImage(
                    fileName = profileImage,
                    size = AvatarSize - 8.dp,
                    modifier = Modifier
                        .background(Color.White, CircleShape)
                        .padding(4.dp)
                        // 프로필 사진을 아래로 쓸어내려도 닫힌다(기존 앱 OnSwipeListener).
                        .swipeDownToClose(onClose = onClose)
                        .clickable(onClick = onAvatarClick),
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(top = AvatarSize / 2 + 16.dp, start = 32.dp, end = 32.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name.ifBlank { stringResource(R.string.profile_name_placeholder) },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnChatBubble,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
            // 이름 옆 별. 차단한 친구에게는 걸 수 없어 회색으로 죽는다.
            if (uiState.showFriendActions) {
                IconButton(
                    onClick = onToggleFavorite,
                    enabled = uiState.canToggleFavorite,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (uiState.favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = stringResource(R.string.profile_favorite_toggle),
                        tint = if (uiState.favorite) FavoriteStarColor else ModuGrey,
                    )
                }
            }
        }
        Text(
            text = statusMessage.ifBlank { stringResource(R.string.profile_status_placeholder) },
            fontSize = 14.sp,
            color = ModuGrey,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp, start = 32.dp, end = 32.dp)
                .fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.showEditButton) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text(stringResource(R.string.profile_edit_button), color = BrandViolet)
                }
            }
            Button(
                onClick = onStartChat,
                enabled = !uiState.startingChat,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Text(stringResource(uiState.startChatTextRes))
            }
        }

        if (uiState.showRenameButton) {
            OutlinedButton(
                onClick = onRename,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.profile_rename_button), color = BrandViolet)
            }
        }
    }
}

/**
 * 상단 우측 ⋮ 메뉴. 숨김·차단은 배타라 각각 걸기/풀기 중 하나만 보인다.
 */
@Composable
private fun FriendMenuButton(
    isHidden: Boolean,
    isBlocked: Boolean,
    enabled: Boolean,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        ScrimIconButton(
            onClick = { if (enabled) expanded = true },
            icon = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.profile_more_menu),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // 차단한 친구는 이미 목록에서 빠져 있으므로 숨기기 항목을 내지 않는다.
            if (!isBlocked) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (isHidden) R.string.profile_menu_unhide else R.string.profile_menu_hide,
                            ),
                        )
                    },
                    onClick = {
                        expanded = false
                        if (isHidden) onUnhide() else onHide()
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isBlocked) R.string.profile_menu_unblock else R.string.profile_menu_block,
                        ),
                    )
                },
                onClick = {
                    expanded = false
                    if (isBlocked) onUnblock() else onBlock()
                },
            )
        }
    }
}

private val FavoriteStarColor = Color(0xFFFFC107)

/** 폭과 같은 정사각형 배경 + 위쪽만 어둡게 까는 스크림. 아래로 쓸어내리면 화면이 닫힌다. */
@Composable
private fun Wallpaper(fileName: String, onClose: () -> Unit, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(BrandViolet)
            .swipeDownToClose(onClose = onClose)
            .clickable(onClick = onClick),
    ) {
        if (fileName.isNotBlank()) {
            AsyncImage(
                model = ApiConfig.imageUrl(fileName),
                contentDescription = stringResource(R.string.profile_wallpaper_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ScrimHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x59000000), Color(0x00000000)),
                    ),
                ),
        )
    }
}

/** 사진 위에 얹는 동그란 아이콘 버튼(`circle_icon_button_scrim` 과 같은 모양). */
@Composable
private fun ScrimIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    // IconButton 은 최소 터치 영역(48dp)을 더해 원이 커 보인다. 원 크기를 그대로 지키려고 Box 로 그린다.
    Box(
        modifier = modifier
            .size(SCRIM_BUTTON_SIZE)
            .clip(CircleShape)
            .background(Color(0x59000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(SCRIM_ICON_SIZE))
    }
}

/** 친구 이름 변경(부록 A §13). 저장 버튼은 이름이 비어 있으면 눌리지 않는다. */
@Composable
private fun RenameFriendDialog(
    value: String,
    canSave: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_rename_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = canSave) {
                Text(stringResource(R.string.profile_rename_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_rename_cancel))
            }
        },
    )
}

/** 배경 위에 뜨는 둥근 아이콘 버튼. 40dp 가 붙어 있으면 답답해 보여 36dp + 간격 10dp 로 둔다. */
private val SCRIM_BUTTON_SIZE = 36.dp
private val SCRIM_ICON_SIZE = 20.dp
private val TOP_BUTTON_GAP = 10.dp
