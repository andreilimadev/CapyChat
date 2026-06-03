package com.andreilima.capychat.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.data.model.Message
import com.andreilima.capychat.data.model.MessageStatus
import com.andreilima.capychat.ui.components.CapyEmptyState

@OptIn(ExperimentalMaterial3Api::class)
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
    onSendMessage: (String) -> Unit,
    onUserTyping: ((String, Boolean, String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Scroll automático ao receber nova mensagem
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
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
                            // Indicador de digitando ou online
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
                    IconButton(onClick = { /* Buscar */ }) {
                        Icon(Icons.Outlined.Search, "Buscar")
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
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Limpar conversa") },
                                leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Fixar conversa") },
                                leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                                onClick = { showMenu = false }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Reportar bug") },
                                leadingIcon = { Icon(Icons.Outlined.BugReport, null) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    IconButton(onClick = { /* Anexo */ }) {
                        Icon(Icons.Outlined.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { newValue ->
                            messageText = newValue
                            // Dispara o indicador de digitando
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

                    Box(contentAlignment = Alignment.Center) {
                        FloatingActionButton(
                            onClick = {
                                if (hasText) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendMessage(messageText.trim())
                                    messageText = ""
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
                                transitionSpec = {
                                    (scaleIn() + fadeIn()).togetherWith(scaleOut() + fadeOut())
                                },
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (messages.isEmpty()) {
                CapyEmptyState(
                    emoji = "💬",
                    title = "Nenhuma mensagem",
                    description = "Inicie a conversa enviando um 'Olá!'"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(messages, key = { _, msg -> msg.id }) { index, message ->
                        val isPreviousFromSame = index > 0 && messages[index - 1].sender == message.sender

                        MessageBubble(
                            message = message,
                            showName = !message.isMine && !isPreviousFromSame,
                            isGrouped = isPreviousFromSame
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    showName: Boolean,
    isGrouped: Boolean
) {
    val bubbleShape = if (message.isMine) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = if (isGrouped) 4.dp else 20.dp,
            bottomEnd = 4.dp,
            bottomStart = 20.dp
        )
    } else {
        RoundedCornerShape(
            topStart = if (isGrouped) 4.dp else 20.dp,
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            bottomStart = 4.dp
        )
    }

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

            Surface(
                color = if (message.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (message.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = bubbleShape,
                tonalElevation = if (message.isMine) 2.dp else 0.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                    // Hora + tick de leitura
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = message.time,
                            fontSize = 10.sp,
                            color = (if (message.isMine) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)
                        )
                        // Ticks de confirmação (só nas mensagens minhas)
                        if (message.isMine) {
                            val tickColor = when (message.status) {
                                MessageStatus.READ -> Color(0xFF4FC3F7)       // azul
                                MessageStatus.DELIVERED -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                MessageStatus.SENT -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            }
                            val tickText = when (message.status) {
                                MessageStatus.SENT -> "✓"
                                MessageStatus.DELIVERED, MessageStatus.READ -> "✓✓"
                            }
                            Text(
                                text = tickText,
                                fontSize = 10.sp,
                                color = tickColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}