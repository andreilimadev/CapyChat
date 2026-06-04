package com.andreilima.capychat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andreilima.capychat.data.firebase.FirebaseService
import com.andreilima.capychat.ui.components.CapyConfirmDialog
import com.andreilima.capychat.ui.components.CapyLoadingState
import com.andreilima.capychat.ui.components.CreateRoomDialogContent
import com.andreilima.capychat.ui.components.MainShell
import com.andreilima.capychat.ui.screens.*
import com.andreilima.capychat.ui.theme.CapyChatTheme
import com.andreilima.capychat.ui.viewmodel.AuthState
import com.andreilima.capychat.ui.viewmodel.AuthViewModel
import com.andreilima.capychat.ui.viewmodel.ChatViewModel
import com.andreilima.capychat.ui.viewmodel.PreferencesViewModel
import kotlinx.coroutines.launch

enum class Screen {
    LOGIN, REGISTER, CONVERSATIONS, CHAT, STATUS, STATUS_VIEW,
    PROFILE, PRIVACY, SETTINGS, HELP, CIA, NOTIFICATIONS
}

@Composable
fun CapyChatApp(
    authViewModel: AuthViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    prefsViewModel: PreferencesViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val darkTheme by prefsViewModel.darkTheme.collectAsStateWithLifecycle()

    CapyChatTheme(darkTheme = darkTheme) {
        var showSplash by remember { mutableStateOf(true) }

        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else {
            val authState by authViewModel.state.collectAsStateWithLifecycle()
            val currentScreenState = remember { mutableStateOf(Screen.LOGIN) }
            var currentScreen by currentScreenState

            var selectedChatId by remember { mutableStateOf<String?>(null) }
            var selectedChatName by remember { mutableStateOf("") }
            var selectedChatEmoji by remember { mutableStateOf("💬") }
            var selectedChatIsPrivate by remember { mutableStateOf(false) }

            var showCreateRoomDialog by remember { mutableStateOf(false) }
            var newRoomName by remember { mutableStateOf("") }
            var newRoomEmoji by remember { mutableStateOf("💬") }

            var selectedStatusId by remember { mutableStateOf<String?>(null) }
            var showLogoutDialog by remember { mutableStateOf(false) }

            val typingUsers by chatViewModel.typingUsers.collectAsStateWithLifecycle()

            fun navigate(to: Screen) { currentScreen = to }
            fun showFeedback(message: String) {
                scope.launch { snackbarHostState.showSnackbar(message) }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    val uid = FirebaseService.auth.currentUser?.uid ?: return@LifecycleEventObserver
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> chatViewModel.setOnline(uid)
                        Lifecycle.Event.ON_PAUSE  -> chatViewModel.setOffline(uid)
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(authState) {
                val state = authState
                if (state is AuthState.LoggedIn) {
                    if (currentScreen == Screen.LOGIN || currentScreen == Screen.REGISTER) {
                        navigate(Screen.CONVERSATIONS)
                        chatViewModel.setOnline(state.uid)
                        chatViewModel.startObservingRooms(state.uid)
                        chatViewModel.startObservingStatuses(state.uid)
                        chatViewModel.startObservingNotifications(state.uid)
                        chatViewModel.loadUserProfile(state.uid)
                    }
                } else if (state is AuthState.LoggedOut) {
                    navigate(Screen.LOGIN)
                    chatViewModel.stopObservingMessages()
                }
            }

            val isMainScreen = currentScreen in listOf(
                Screen.CONVERSATIONS, Screen.STATUS, Screen.PROFILE, Screen.NOTIFICATIONS
            )

            if (authState is AuthState.Loading) {
                CapyLoadingState(message = "Autenticando...")
            } else {
                val loggedInState = authState as? AuthState.LoggedIn
                val rooms by chatViewModel.rooms.collectAsStateWithLifecycle()
                val messages by chatViewModel.messages.collectAsStateWithLifecycle()
                val statuses by chatViewModel.statuses.collectAsStateWithLifecycle()
                val searchResults by chatViewModel.searchResults.collectAsStateWithLifecycle()
                val isSearching by chatViewModel.isSearching.collectAsStateWithLifecycle()
                val searchQuery by chatViewModel.searchQuery.collectAsStateWithLifecycle()
                val unreadCount by chatViewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

                if (isMainScreen) {
                    MainShell(
                        currentScreen = currentScreen,
                        onNavigate = { navigate(it) },
                        snackbarHostState = snackbarHostState,
                        unreadNotificationsCount = unreadCount
                    ) { padding ->
                        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "main_transition"
                            ) { screen ->
                                when (screen) {
                                    Screen.CONVERSATIONS -> ConversationsScreen(
                                        rooms = rooms,
                                        searchResults = searchResults,
                                        isSearching = isSearching,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { chatViewModel.onSearchQueryChange(it) },
                                        onChatClick = { chat ->
                                            selectedChatId = chat.id
                                            selectedChatName = chat.name
                                            selectedChatEmoji = chat.avatarEmoji
                                            selectedChatIsPrivate = chat.isPrivate
                                            loggedInState?.let { state ->
                                                chatViewModel.startObservingMessages(chat.id, chat.isPrivate, state.uid)
                                            }
                                            navigate(Screen.CHAT)
                                        },
                                        onUserClick = { user ->
                                            loggedInState?.let { state ->
                                                chatViewModel.startPrivateChat(state.uid, state.username, user) { roomId, roomName ->
                                                    selectedChatId = roomId
                                                    selectedChatName = roomName
                                                    selectedChatEmoji = "🔒"
                                                    selectedChatIsPrivate = true
                                                    chatViewModel.startObservingMessages(roomId, true, state.uid)
                                                    navigate(Screen.CHAT)
                                                    showFeedback("Conversa com $roomName iniciada!")
                                                }
                                            }
                                        },
                                        onCreateRoom = { showCreateRoomDialog = true }
                                    )
                                    Screen.STATUS -> StatusScreen(
                                        statuses = statuses,
                                        onStatusClick = { status ->
                                            selectedStatusId = status.id
                                            navigate(Screen.STATUS_VIEW)
                                        },
                                        onPostStatus = { text, emoji ->
                                            loggedInState?.let { state ->
                                                chatViewModel.createStatus(state.uid, state.username, text, emoji) {
                                                    showFeedback("Status publicado!")
                                                }
                                            }
                                        }
                                    )
                                    Screen.NOTIFICATIONS -> NotificationsScreen(
                                        onNotificationClick = { notif ->
                                            if (notif.type == "message" && notif.payload.isNotEmpty()) {
                                                navigate(Screen.CONVERSATIONS)
                                            }
                                        }
                                    )
                                    Screen.PROFILE -> ProfileScreen(
                                        userName = loggedInState?.username ?: "Usuário",
                                        userEmail = FirebaseService.auth.currentUser?.email ?: "",
                                        onLogout = { showLogoutDialog = true },
                                        onOpenPrivacy = { navigate(Screen.PRIVACY) },
                                        onOpenSettings = { navigate(Screen.SETTINGS) },
                                        onOpenHelp = { navigate(Screen.HELP) },
                                        onOpenCIA = {
                                            navigate(Screen.CIA)
                                            showFeedback("CIA ACCESS GRANTED")
                                        }
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
                            },
                            label = "secondary_transition"
                        ) { screen ->
                            when (screen) {
                                Screen.LOGIN -> LoginScreen(
                                    onLoginClick = { e, p -> authViewModel.login(e, p) },
                                    onGoToRegister = { navigate(Screen.REGISTER) }
                                )
                                Screen.REGISTER -> RegisterScreen(
                                    onRegisterClick = { n, e, p -> authViewModel.register(n, e, p) },
                                    onBackToLogin = { navigate(Screen.LOGIN) }
                                )
                                Screen.CHAT -> {
                                    selectedChatId?.let { chatId ->
                                        ChatScreen(
                                            contactName = selectedChatName,
                                            contactEmoji = selectedChatEmoji,
                                            messages = messages,
                                            typingUsers = typingUsers,
                                            roomId = chatId,
                                            isPrivate = selectedChatIsPrivate,
                                            currentUserId = loggedInState?.uid ?: "",
                                            onBackClick = {
                                                chatViewModel.stopObservingMessages()
                                                navigate(Screen.CONVERSATIONS)
                                            },
                                            onSendMessage = { text, replyTo ->
                                                loggedInState?.let { state ->
                                                    chatViewModel.sendMessage(
                                                        roomId = chatId,
                                                        isPrivate = selectedChatIsPrivate,
                                                        text = text,
                                                        senderId = state.uid,
                                                        senderName = state.username,
                                                        replyToText = replyTo?.text ?: "",
                                                        replyToSender = replyTo?.sender ?: ""
                                                    )
                                                }
                                            },
                                            onReactToMessage = { messageId, emoji, currentEmoji ->
                                                loggedInState?.let { state ->
                                                    chatViewModel.reactToMessage(
                                                        roomId = chatId,
                                                        isPrivate = selectedChatIsPrivate,
                                                        messageId = messageId,
                                                        userId = state.uid,
                                                        emoji = emoji,
                                                        currentEmoji = currentEmoji
                                                    )
                                                }
                                            },
                                            onUserTyping = { roomId, isPrivate, userId ->
                                                chatViewModel.onUserTyping(roomId, isPrivate, userId)
                                            }
                                        )
                                    } ?: navigate(Screen.CONVERSATIONS)
                                }
                                Screen.STATUS_VIEW -> {
                                    val statusIndex = statuses.indexOfFirst { it.id == selectedStatusId }
                                    val currentStatus = if (statusIndex >= 0) statuses[statusIndex] else null
                                    currentStatus?.let { status ->
                                        LaunchedEffect(status.id) {
                                            chatViewModel.markStatusAsViewed(status.id, loggedInState?.uid ?: "")
                                        }
                                        StatusViewerScreen(
                                            status = status,
                                            allStatuses = statuses,
                                            currentIndex = statusIndex,
                                            onBackClick = { navigate(Screen.STATUS) },
                                            onNextClick = {
                                                val next = statuses.getOrNull(statusIndex + 1)
                                                if (next != null) selectedStatusId = next.id
                                                else navigate(Screen.STATUS)
                                            }
                                        )
                                    } ?: navigate(Screen.STATUS)
                                }
                                Screen.PRIVACY -> PrivacyScreen(onBackClick = { navigate(Screen.PROFILE) })
                                Screen.SETTINGS -> SettingsScreen(onBackClick = { navigate(Screen.PROFILE) })
                                Screen.HELP -> HelpScreen(onBackClick = { navigate(Screen.PROFILE) })
                                Screen.CIA -> CIASecretScreen(onBackClick = { navigate(Screen.PROFILE) })
                                else -> {}
                            }
                        }
                        SnackbarHost(hostState = snackbarHostState)
                    }
                }

                if (showCreateRoomDialog) {
                    AlertDialog(
                        onDismissRequest = { showCreateRoomDialog = false },
                        title = { Text("Nova Sala", fontWeight = FontWeight.Bold) },
                        text = {
                            CreateRoomDialogContent(
                                roomName = newRoomName,
                                roomEmoji = newRoomEmoji,
                                onNameChange = { newRoomName = it },
                                onEmojiChange = { newRoomEmoji = it }
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newRoomName.isNotBlank()) {
                                        loggedInState?.let { state ->
                                            chatViewModel.createPublicRoom(newRoomName, newRoomEmoji, state.username) { roomId ->
                                                selectedChatId = roomId
                                                selectedChatName = newRoomName
                                                selectedChatEmoji = newRoomEmoji
                                                selectedChatIsPrivate = false
                                                chatViewModel.startObservingMessages(roomId, false, state.uid)
                                                navigate(Screen.CHAT)
                                                showFeedback("Sala '$newRoomName' criada!")
                                            }
                                        }
                                        showCreateRoomDialog = false
                                        newRoomName = ""
                                    }
                                }
                            ) { Text("Criar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCreateRoomDialog = false }) { Text("Cancelar") }
                        }
                    )
                }

                if (showLogoutDialog) {
                    CapyConfirmDialog(
                        title = "Sair da conta",
                        message = "Deseja realmente sair?",
                        onConfirm = {
                            showLogoutDialog = false
                            authViewModel.logout()
                        },
                        onDismiss = { showLogoutDialog = false }
                    )
                }
            }
        }
    }
}