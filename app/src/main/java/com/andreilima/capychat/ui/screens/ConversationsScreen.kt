package com.andreilima.capychat.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.data.model.ChatItem
import com.andreilima.capychat.data.model.UserSearchItem
import com.andreilima.capychat.ui.components.CapyTopBar
import com.andreilima.capychat.ui.utils.pressEffect
import kotlin.math.roundToInt

// ─── 1. TELA PRINCIPAL ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    rooms: List<ChatItem>,
    searchResults: List<UserSearchItem>,
    isSearching: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onChatClick: (ChatItem) -> Unit,
    onUserClick: (UserSearchItem) -> Unit,
    onCreateRoom: () -> Unit,
    onPinChat: (ChatItem) -> Unit = {},
    onMuteChat: (ChatItem) -> Unit = {},
    onDeleteChat: (ChatItem) -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    var searchText by remember { mutableStateOf("") }
    var isFabExpanded by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filteredRooms = remember(rooms, searchText) {
        rooms.filter { room ->
            val query = searchText.trim()
            query.isBlank() || room.name.contains(query, ignoreCase = true) ||
                    room.lastMessage.contains(query, ignoreCase = true)
        }.sortedWith(compareByDescending { it.isPinned })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CapyTopBar(title = "CapyChat")

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Buscar conversas...") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            if (filteredRooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nenhuma conversa encontrada",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredRooms, key = { it.id }) { room ->
                        SwipeableChatItem(
                            chat = room,
                            onClick = { onChatClick(room) },
                            onPin = { onPinChat(room) },
                            onMute = { onMuteChat(room) },
                            onDelete = { onDeleteChat(room) },
                            haptic = haptic
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }

        // FAB
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniFabWithLabel(
                        label = "Nova conversa",
                        icon = Icons.Outlined.PersonAdd,
                        onClick = { isFabExpanded = false; showSearchDialog = true }
                    )
                    MiniFabWithLabel(
                        label = "Nova sala",
                        icon = Icons.Outlined.Add,
                        onClick = { isFabExpanded = false; onCreateRoom() }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isFabExpanded = !isFabExpanded
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (isFabExpanded) 45f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "fab_rotation"
                )
                Icon(Icons.Outlined.Add, contentDescription = "Menu",
                    modifier = Modifier.graphicsLayer(rotationZ = rotation))
            }
        }
    }

    if (showSearchDialog) {
        UserSearchDialog(
            query = searchQuery,
            results = searchResults,
            isSearching = isSearching,
            onQueryChange = onSearchQueryChange,
            onUserClick = { showSearchDialog = false; onUserClick(it) },
            onDismiss = { showSearchDialog = false }
        )
    }
}

// ─── 2. SWIPE WRAPPER ────────────────────────────────────────────────────────

@Composable
fun SwipeableChatItem(
    chat: ChatItem,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onMute: () -> Unit,
    onDelete: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val swipeThreshold = 100f
    val maxSwipe = 160f
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "swipe_offset"
    )

    LaunchedEffect(chat.id) { offsetX = 0f }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Fundo esquerda → fixar + mutar
        if (animatedOffset > 0f) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .width((animatedOffset / maxSwipe * 140).dp.coerceAtMost(140.dp))
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val iconsAlpha = ((animatedOffset - 40f) / 60f).coerceIn(0f, 1f)
                IconButton(onClick = { onPin(); offsetX = 0f },
                    modifier = Modifier.graphicsLayer(alpha = iconsAlpha)) {
                    Icon(Icons.Outlined.PushPin,
                        contentDescription = if (chat.isPinned) "Desafixar" else "Fixar",
                        tint = if (chat.isPinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSecondaryContainer)
                }
                IconButton(onClick = { onMute(); offsetX = 0f },
                    modifier = Modifier.graphicsLayer(alpha = iconsAlpha)) {
                    Icon(if (chat.isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = if (chat.isMuted) "Ativar som" else "Mutar",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        // Fundo direita → deletar
        if (animatedOffset < 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .width((-animatedOffset / maxSwipe * 100).dp.coerceAtMost(100.dp))
                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                val iconAlpha = ((-animatedOffset - 40f) / 60f).coerceIn(0f, 1f)
                IconButton(onClick = { onDelete(); offsetX = 0f },
                    modifier = Modifier.graphicsLayer(alpha = iconAlpha)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Deletar",
                        tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // Item arrastável
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(chat.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = when {
                                offsetX > swipeThreshold  ->  maxSwipe
                                offsetX < -swipeThreshold -> -maxSwipe
                                else -> 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = (offsetX + dragAmount).coerceIn(-maxSwipe, maxSwipe)
                            if (offsetX == 0f && newOffset != 0f)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            offsetX = newOffset
                        }
                    )
                }
        ) {
            ChatListItem(
                chat = chat,
                onClick = { if (offsetX != 0f) offsetX = 0f else onClick() }
            )
        }
    }
}

// ─── 3. CHAT LIST ITEM ───────────────────────────────────────────────────────

@Composable
fun ChatListItem(chat: ChatItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().pressEffect(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(chat.avatarEmoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (chat.isPinned) {
                            Icon(Icons.Outlined.PushPin, contentDescription = null,
                                modifier = Modifier.size(12.dp).padding(end = 2.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = chat.name,
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (chat.isMuted) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeOff, contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                        Text(text = chat.lastTime, fontSize = 11.sp,
                            color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage, fontSize = 13.sp,
                        color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (chat.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                color = Color.White, fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 4. DIALOGS E HELPERS ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchDialog(
    query: String,
    results: List<UserSearchItem>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onUserClick: (UserSearchItem) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.85f)) {
        Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Nova Conversa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = query, onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por @username...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    shape = RoundedCornerShape(16.dp), singleLine = true)
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isSearching -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(5) { SearchSkeletonItem() }
                        }
                        results.isEmpty() && query.length >= 3 ->
                            Text("Nenhum usuário encontrado", modifier = Modifier.align(Alignment.Center))
                        query.length < 3 ->
                            Text("Digite ao menos 3 letras", modifier = Modifier.align(Alignment.Center))
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(results) { user -> UserResultItem(user = user, onClick = { onUserClick(user) }) }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancelar") }
            }
        }
    }
}

@Composable
fun UserResultItem(user: UserSearchItem, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) { Text("🐾", fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(user.username, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("@${user.userTag}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SearchSkeletonItem() {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.width(120.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
            Box(modifier = Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)))
        }
    }
}

@Composable
fun MiniFabWithLabel(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 4.dp) {
            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(12.dp))
        SmallFloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, null)
        }
    }
}