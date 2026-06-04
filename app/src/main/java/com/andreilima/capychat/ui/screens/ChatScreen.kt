package com.andreilima.capychat.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andreilima.capychat.data.model.Message
import com.andreilima.capychat.data.model.MessageStatus
import com.andreilima.capychat.ui.components.CapyEmptyState
import com.andreilima.capychat.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    contactName: String,
    contactEmoji: String,
    messages: List<Message>,
    isContactOnline: Boolean = false,
    typingUsers: List<String> = emptyList(),
    roomId: String = "",
    isPrivate: Boolean = false,
    currentUserId: String = "",
    onBackClick: () -> Unit,
    onSendMessage: (String, Message?) -> Unit,
    onReactToMessage: (String, String, String?) -> Unit = { _, _, _ -> },
    onUserTyping: ((String, Boolean, String) -> Unit)? = null,
    chatViewModel: ChatViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) messages
        else messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }
    val displayMessages = if (showSearch && searchQuery.isNotBlank()) filteredMessages else messages

    var showClearDialog by remember { mutableStateOf(false) }
    var showBugDialog by remember { mutableStateOf(false) }
    var bugDescription by remember { mutableStateOf("") }

    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !showSearch) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Limpar conversa") },
            text = { Text("Todas as mensagens serão apagadas permanentemente. Tem certeza?") },
            confirmButton = {
                Button(
                    onClick = {
                        chatViewModel.clearMessages(roomId, isPrivate) {}
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Limpar") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showBugDialog) {
        AlertDialog(
            onDismissRequest = { showBugDialog = false },
            icon = { Icon(Icons.Outlined.BugReport, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Reportar Bug") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Descreva o problema que encontrou:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = bugDescription,
                        onValueChange = { bugDescription = it },
                        placeholder = { Text("Ex: mensagens não carregam...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bugDescription.isNotBlank()) {
                            chatViewModel.reportBug(currentUserId, bugDescription, roomId) {}
                            bugDescription = ""
                            showBugDialog = false
                        }
                    },
                    enabled = bugDescription.isNotBlank()
                ) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { showBugDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(contactEmoji, fontSize = 20.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    contactName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val statusText = when {
                                    typingUsers.isNotEmpty() -> "digitando..."
                                    isContactOnline -> "online agora"
                                    else -> ""
                                }
                                AnimatedVisibility(visible = statusText.isNotEmpty()) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (typingUsers.isNotEmpty())
                                            MaterialTheme.colorScheme.tertiary
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showSearch = !showSearch
                            if (!showSearch) searchQuery = ""
                        }) {
                            Icon(
                                if (showSearch) Icons.Outlined.SearchOff else Icons.Outlined.Search,
                                "Buscar"
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Outlined.MoreVert, "Mais")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ver mídia") },
                                    leadingIcon = { Icon(Icons.Outlined.PermMedia, null) },
                                    onClick = { showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Silenciar") },
                                    leadingIcon = { Icon(Icons.Outlined.NotificationsOff, null) },
                                    onClick = {
                                        showMenu = false
                                        chatViewModel.muteRoom(roomId, isPrivate, currentUserId, true) {}
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Fixar conversa") },
                                    leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                                    onClick = {
                                        showMenu = false
                                        chatViewModel.pinRoom(roomId, isPrivate, currentUserId, true) {}
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Limpar conversa") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) },
                                    onClick = {
                                        showMenu = false
                                        showClearDialog = true
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Reportar bug") },
                                    leadingIcon = { Icon(Icons.Outlined.BugReport, null) },
                                    onClick = {
                                        showMenu = false
                                        showBugDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                AnimatedVisibility(
                    visible = showSearch,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar na conversa...") },
                            leadingIcon = { Icon(Icons.Outlined.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Outlined.Clear, null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "${filteredMessages.size} resultado(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column {
                    AnimatedVisibility(
                        visible = replyingTo != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        replyingTo?.let { reply ->
                            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            reply.sender,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            reply.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { replyingTo = null },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        IconButton(onClick = { /* Bloco 4 — anexos */ }) {
                            Icon(Icons.Outlined.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { newValue ->
                                messageText = newValue
                                if (newValue.isNotBlank() && roomId.isNotEmpty() && currentUserId.isNotEmpty()) {
                                    onUserTyping?.invoke(roomId, isPrivate, currentUserId)
                                }
                            },
                            placeholder = { Text("Mensagem") },
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize(),
                            shape = RoundedCornerShape(28.dp),
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        val hasText = messageText.isNotBlank()
                        FloatingActionButton(
                            onClick = {
                                if (hasText) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendMessage(messageText.trim(), replyingTo)
                                    messageText = ""
                                    replyingTo = null
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                            shape = CircleShape
                        ) {
                            AnimatedContent(
                                targetState = hasText,
                                transitionSpec = { (scaleIn() + fadeIn()).togetherWith(scaleOut() + fadeOut()) },
                                label = "send_icon"
                            ) { isSending ->
                                Icon(
                                    imageVector = if (isSending) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.Mic,
                                    contentDescription = if (isSending) "Enviar" else "Gravar"
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (displayMessages.isEmpty()) {
                CapyEmptyState(
                    emoji = if (showSearch && searchQuery.isNotBlank()) "🔍" else "💬",
                    title = if (showSearch && searchQuery.isNotBlank()) "Nenhum resultado" else "Nenhuma mensagem",
                    description = if (showSearch && searchQuery.isNotBlank()) "Tente buscar por outra palavra" else "Inicie a conversa enviando um 'Olá!'"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        displayMessages,
                        key = { index, msg -> msg.id.ifBlank { "msg_$index" } }
                    ) { index, message ->
                        val isPreviousFromSame = index > 0 && displayMessages[index - 1].sender == message.sender
                        val isHighlighted = showSearch && searchQuery.isNotBlank() &&
                                message.text.contains(searchQuery, ignoreCase = true)

                        MessageBubble(
                            message = message,
                            showName = !message.isMine && !isPreviousFromSame,
                            isGrouped = isPreviousFromSame,
                            isHighlighted = isHighlighted,
                            currentUserId = currentUserId,
                            onReply = { replyingTo = it },
                            onReact = { msg, emoji ->
                                val currentEmoji = msg.reactions[currentUserId]
                                onReactToMessage(msg.id, emoji, currentEmoji)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    showName: Boolean,
    isGrouped: Boolean,
    isHighlighted: Boolean = false,
    currentUserId: String = "",
    onReply: (Message) -> Unit = {},
    onReact: (Message, String) -> Unit = { _, _ -> }
) {
    val bubbleShape = if (message.isMine) {
        RoundedCornerShape(topStart = 20.dp, topEnd = if (isGrouped) 4.dp else 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp)
    } else {
        RoundedCornerShape(topStart = if (isGrouped) 4.dp else 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
    }

    val reactionEmojis = listOf("❤️", "😂", "👍", "😮", "😢", "🔥")
    var showActionMenu by remember { mutableStateOf(false) }
    val myCurrentEmoji = message.reactions[currentUserId]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isGrouped) 2.dp else 8.dp)
            .animateContentSize(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (showName) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                )
            }

            Box {
                Surface(
                    color = when {
                        isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                        message.isMine -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = when {
                        isHighlighted -> MaterialTheme.colorScheme.onTertiaryContainer
                        message.isMine -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = bubbleShape,
                    tonalElevation = if (message.isMine) 2.dp else 0.dp,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showActionMenu = true }
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {

                        if (message.replyToText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (message.isMine)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(modifier = Modifier.padding(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(32.dp)
                                            .background(
                                                if (message.isMine)
                                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                                else
                                                    MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            message.replyToSender,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            message.replyToText,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Text(text = message.text, fontSize = 15.sp, lineHeight = 20.sp)

                        Row(
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = message.time,
                                fontSize = 10.sp,
                                color = (if (message.isMine) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)
                            )
                            if (message.isMine) {
                                val tickColor = when (message.status) {
                                    MessageStatus.READ -> Color(0xFF4FC3F7)
                                    MessageStatus.DELIVERED -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    MessageStatus.SENT -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                }
                                Text(
                                    text = when (message.status) {
                                        MessageStatus.SENT -> "✓"
                                        else -> "✓✓"
                                    },
                                    fontSize = 10.sp,
                                    color = tickColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showActionMenu,
                    onDismissRequest = { showActionMenu = false }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reactionEmojis.forEach { emoji ->
                            val isSelected = emoji == myCurrentEmoji
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            Color.Transparent
                                    )
                                    .clickable {
                                        onReact(message, emoji)
                                        showActionMenu = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Responder") },
                        leadingIcon = { Icon(Icons.Outlined.Reply, null) },
                        onClick = {
                            onReply(message)
                            showActionMenu = false
                        }
                    )
                }
            }

            if (message.reactions.isNotEmpty()) {
                val grouped = message.reactions.values
                    .groupBy { it }
                    .mapValues { it.value.size }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                ) {
                    grouped.forEach { (emoji, count) ->
                        val isMine = myCurrentEmoji == emoji
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMine)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = if (isMine) 4.dp else 2.dp,
                            modifier = Modifier.clickable { onReact(message, emoji) }
                        ) {
                            Text(
                                text = if (count > 1) "$emoji $count" else emoji,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 12.sp,
                                color = if (isMine)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}