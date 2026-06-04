package com.andreilima.capychat.ui.screens

import android.net.Uri
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andreilima.capychat.data.firebase.MediaRepository
import com.andreilima.capychat.data.model.Message
import com.andreilima.capychat.ui.chat.*
import com.andreilima.capychat.ui.components.CapyEmptyState
import com.andreilima.capychat.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
    onSendMessage: (String, Message?) -> Unit,
    onReactToMessage: (String, String, String?) -> Unit = { _, _, _ -> },
    onUserTyping: ((String, Boolean, String) -> Unit)? = null,
    chatViewModel: ChatViewModel = viewModel()
) {
    var uploadProgress by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
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
    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()

    fun handleUpload(uri: Uri, type: String) {
        val fileName = "${System.currentTimeMillis()}_${uri.lastPathSegment ?: "file"}"
        val path = when (type) {
            "image" -> MediaRepository.imagePathForRoom(roomId, fileName)
            "video" -> MediaRepository.videoPathForRoom(roomId, fileName)
            "audio" -> MediaRepository.audioPathForRoom(roomId, fileName)
            else    -> MediaRepository.filePathForRoom(roomId, fileName)
        }
        MediaRepository.uploadFile(uri, path)
            .onEach { state ->
                when (state) {
                    is MediaRepository.UploadState.Progress -> uploadProgress = state.percent
                    is MediaRepository.UploadState.Success  -> {
                        uploadProgress = null
                        // Envia a URL como texto, mas com messageType correto
                        chatViewModel.sendMessage(
                            roomId = roomId,
                            isPrivate = isPrivate,
                            text = state.downloadUrl,
                            senderId = currentUserId,
                            senderName = currentUserId,
                            messageType = type   // ← "image", "video", "file"
                        )
                    }
                    is MediaRepository.UploadState.Error -> uploadProgress = null
                }
            }
            .launchIn(scope)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !showSearch) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (showClearDialog) {
        ClearConversationDialog(
            onConfirm = { chatViewModel.clearMessages(roomId, isPrivate) {}; showClearDialog = false },
            onDismiss = { showClearDialog = false }
        )
    }
    if (showBugDialog) {
        BugReportDialog(
            onConfirm = { desc ->
                chatViewModel.reportBug(currentUserId, desc, roomId) {}
                showBugDialog = false
            },
            onDismiss = { showBugDialog = false }
        )
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                contactName = contactName,
                contactEmoji = contactEmoji,
                isContactOnline = isContactOnline,
                typingUsers = typingUsers,
                showSearch = showSearch,
                searchQuery = searchQuery,
                filteredCount = filteredMessages.size,
                onBackClick = onBackClick,
                onToggleSearch = { showSearch = !showSearch; if (!showSearch) searchQuery = "" },
                onSearchQueryChange = { searchQuery = it },
                onMute = { chatViewModel.muteRoom(roomId, isPrivate, currentUserId, true) {} },
                onPin = { chatViewModel.pinRoom(roomId, isPrivate, currentUserId, true) {} },
                onClearConversation = { showClearDialog = true },
                onReportBug = { showBugDialog = true }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column {
                    UploadProgressBar(progress = uploadProgress)
                    ReplyPreview(
                        replyingTo = replyingTo,
                        onCancelReply = { replyingTo = null }
                    )
                    MessageInput(
                        messageText = messageText,
                        onMessageChange = { newValue ->
                            messageText = newValue
                            if (newValue.isNotBlank() && roomId.isNotEmpty() && currentUserId.isNotEmpty()) {
                                onUserTyping?.invoke(roomId, isPrivate, currentUserId)
                            }
                        },
                        onSendClick = {
                            onSendMessage(messageText.trim(), replyingTo)
                            messageText = ""
                            replyingTo = null
                        },
                        onAttachClick = {},
                        onImageSelected = { uri -> handleUpload(uri, "image") },
                        onVideoSelected = { uri -> handleUpload(uri, "video") },
                        onFileSelected = { uri -> handleUpload(uri, "file") }
                    )
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