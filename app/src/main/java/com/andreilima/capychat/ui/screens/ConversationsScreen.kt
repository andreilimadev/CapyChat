package com.andreilima.capychat.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.data.model.ChatItem
import com.andreilima.capychat.data.model.UserSearchItem
import com.andreilima.capychat.ui.components.CapyTopBar
import com.andreilima.capychat.ui.utils.pressEffect

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
        }
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
                    items(filteredRooms) { room ->
                        ChatListItem(chat = room, onClick = { onChatClick(room) })
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
                        onClick = {
                            isFabExpanded = false
                            showSearchDialog = true
                        }
                    )
                    MiniFabWithLabel(
                        label = "Nova sala",
                        icon = Icons.Outlined.Add,
                        onClick = {
                            isFabExpanded = false
                            onCreateRoom()
                        }
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
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Menu",
                    modifier = Modifier.graphicsLayer(rotationZ = rotation)
                )
            }
        }
    }

    if (showSearchDialog) {
        UserSearchDialog(
            query = searchQuery,
            results = searchResults,
            isSearching = isSearching,
            onQueryChange = onSearchQueryChange,
            onUserClick = {
                showSearchDialog = false
                onUserClick(it)
            },
            onDismiss = { showSearchDialog = false }
        )
    }
}

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
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Nova Conversa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por @username...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isSearching -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                repeat(5) { SearchSkeletonItem() }
                            }
                        }
                        results.isEmpty() && query.length >= 3 -> {
                            Text(
                                "Nenhum usuário encontrado",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        query.length < 3 -> {
                            Text(
                                "Digite ao menos 3 letras",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        else -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(results) { user ->
                                    UserResultItem(user = user, onClick = { onUserClick(user) })
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
fun UserResultItem(user: UserSearchItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("🐾", fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                user.username,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "@${user.userTag}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SearchSkeletonItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun MiniFabWithLabel(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 4.dp
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(Modifier.width(12.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(icon, null)
        }
    }
}

@Composable
fun ChatListItem(chat: ChatItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressEffect(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(chat.avatarEmoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        chat.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        chat.lastTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    chat.lastMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}