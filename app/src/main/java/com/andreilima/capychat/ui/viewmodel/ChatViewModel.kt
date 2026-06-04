package com.andreilima.capychat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andreilima.capychat.data.firebase.FirebaseService
import com.andreilima.capychat.data.model.*
import com.andreilima.capychat.ui.state.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    // =========================================================
    // STATES TIPADOS
    // =========================================================

    private val _conversationState = MutableStateFlow(ConversationUiState())
    val conversationState: StateFlow<ConversationUiState> = _conversationState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _profileState = MutableStateFlow(ProfileUiState())
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    private val _statusState = MutableStateFlow(StatusUiState())
    val statusState: StateFlow<StatusUiState> = _statusState.asStateFlow()

    private val _notificationState = MutableStateFlow(NotificationUiState())
    val notificationState: StateFlow<NotificationUiState> = _notificationState.asStateFlow()

    // =========================================================
    // COMPATIBILIDADE (mantidas para não quebrar as telas ainda)
    // =========================================================

    val rooms: StateFlow<List<ChatItem>> = conversationState
        .map { it.rooms.getDataOrNull() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val messages: StateFlow<List<Message>> = chatState
        .map { it.messages.getDataOrNull() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val statuses: StateFlow<List<StatusItem>> = statusState
        .map { it.statuses.getDataOrNull() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isLoading: StateFlow<Boolean> = chatState
        .map { it.isSending || it.messages is UiState.Loading }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isProfileSaving: StateFlow<Boolean> = _profileState
        .map { it.isSaving }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val typingUsers: StateFlow<List<String>> = chatState
        .map { it.typingUsers }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val searchQuery: StateFlow<String> = conversationState
        .map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val searchResults: StateFlow<List<UserSearchItem>> = conversationState
        .map { it.searchResults }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isSearching: StateFlow<Boolean> = conversationState
        .map { it.isSearching }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val currentUserProfile: StateFlow<FirestoreUser?> = profileState
        .map { it.user.getDataOrNull() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val notifications: StateFlow<List<FirestoreNotification>> = notificationState
        .map { it.notifications.getDataOrNull() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notificationState
        .map { it.unreadCount }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // =========================================================
    // JOBS
    // =========================================================

    private var currentRoomId: String? = null
    private var currentRoomIsPrivate: Boolean = false
    private var currentUserId: String? = null
    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var typingResetJob: Job? = null
    private var searchJob: Job? = null

    init { startSearchQueryObservation() }

    // =========================================================
    // USER SEARCH
    // =========================================================

    fun onSearchQueryChange(newQuery: String) {
        _conversationState.update { it.copy(searchQuery = newQuery) }
    }

    @OptIn(FlowPreview::class)
    private fun startSearchQueryObservation() {
        viewModelScope.launch {
            _conversationState
                .map { it.searchQuery }
                .debounce(500)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 3) performSearch(query)
                    else _conversationState.update { it.copy(searchResults = emptyList()) }
                }
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _conversationState.update { it.copy(isSearching = true) }
            val users = FirebaseService.searchUsers(query)
            _conversationState.update {
                it.copy(
                    searchResults = users.map { u -> u.toSearchItem() },
                    isSearching = false
                )
            }
        }
    }

    // =========================================================
    // PERFIL
    // =========================================================

    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            FirebaseService.observeUser(uid).collect { user ->
                _profileState.update {
                    it.copy(user = if (user != null) UiState.Success(user) else UiState.Empty)
                }
            }
        }
    }

    // =========================================================
    // ONLINE STATUS
    // =========================================================

    fun setOnline(uid: String) {
        viewModelScope.launch { FirebaseService.setOnlineStatus(uid, true) }
    }

    fun setOffline(uid: String) {
        viewModelScope.launch { FirebaseService.setOnlineStatus(uid, false) }
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch { FirebaseService.logout(); onDone() }
    }

    // =========================================================
    // ROOMS
    // =========================================================

    fun startObservingRooms(uid: String) {
        currentUserId = uid
        viewModelScope.launch {
            _conversationState.update { it.copy(rooms = UiState.Loading) }
            FirebaseService.observeRooms(uid).collect { pairs ->
                val items = pairs.map { (id, room) -> room.toChatItem(id, uid) }
                _conversationState.update {
                    it.copy(rooms = if (items.isEmpty()) UiState.Empty else UiState.Success(items))
                }
            }
        }
    }

    // =========================================================
    // MESSAGES
    // =========================================================

    fun startObservingMessages(roomId: String, isPrivate: Boolean, currentUserId: String) {
        this.currentRoomId = roomId
        this.currentRoomIsPrivate = isPrivate
        this.currentUserId = currentUserId

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            _chatState.update { it.copy(messages = UiState.Loading) }
            FirebaseService.observeMessages(roomId, isPrivate).collect { pairs ->
                val msgs = pairs.map { (id, msg) -> msg.toMessage(id, currentUserId) }
                _chatState.update {
                    it.copy(messages = if (msgs.isEmpty()) UiState.Empty else UiState.Success(msgs))
                }
                pairs.forEach { (id, msg) ->
                    if (msg.senderId != currentUserId && !msg.readBy.containsKey(currentUserId)) {
                        FirebaseService.markMessageAsRead(roomId, isPrivate, id, currentUserId)
                    }
                }
            }
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            FirebaseService.observeTyping(roomId, isPrivate, currentUserId).collect { users ->
                _chatState.update { it.copy(typingUsers = users) }
            }
        }
    }

    fun stopObservingMessages() {
        currentRoomId?.let { roomId ->
            currentUserId?.let { uid ->
                viewModelScope.launch { FirebaseService.setTyping(roomId, currentRoomIsPrivate, uid, false) }
            }
        }
        messagesJob?.cancel()
        typingJob?.cancel()
        typingResetJob?.cancel()
        _chatState.update { it.copy(messages = UiState.Empty, typingUsers = emptyList()) }
        currentRoomId = null
    }

    // =========================================================
    // DIGITANDO
    // =========================================================

    fun onUserTyping(roomId: String, isPrivate: Boolean, userId: String) {
        viewModelScope.launch { FirebaseService.setTyping(roomId, isPrivate, userId, true) }
        typingResetJob?.cancel()
        typingResetJob = viewModelScope.launch {
            delay(3000)
            FirebaseService.setTyping(roomId, isPrivate, userId, false)
        }
    }

    // =========================================================
    // SEND MESSAGE
    // =========================================================

    fun sendMessage(
        roomId: String,
        isPrivate: Boolean,
        text: String,
        senderId: String,
        senderName: String,
        messageType: String = "text",   // ← adicionar este parâmetro
        replyToText: String = "",
        replyToSender: String = ""
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _chatState.update { it.copy(isSending = true) }
            FirebaseService.setTyping(roomId, isPrivate, senderId, false)
            typingResetJob?.cancel()
            FirebaseService.sendMessage(
                roomId = roomId,
                isPrivate = isPrivate,
                text = text.trim(),
                senderId = senderId,
                senderName = senderName,
                messageType = messageType,  // ← passar aqui
                replyToText = replyToText,
                replyToSender = replyToSender
            )
            _chatState.update { it.copy(isSending = false) }
        }
    }

    // =========================================================
    // REACTIONS
    // =========================================================

    fun reactToMessage(
        roomId: String, isPrivate: Boolean, messageId: String,
        userId: String, emoji: String, currentEmoji: String? = null
    ) {
        viewModelScope.launch {
            FirebaseService.reactToMessage(roomId, isPrivate, messageId, userId, emoji, currentEmoji)
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    fun startObservingStatuses(currentUserId: String) {
        viewModelScope.launch {
            _statusState.update { it.copy(statuses = UiState.Loading) }
            FirebaseService.observeStatuses().collect { list ->
                val items = list.map { it.toStatusItem(currentUserId) }
                _statusState.update {
                    it.copy(statuses = if (items.isEmpty()) UiState.Empty else UiState.Success(items))
                }
            }
        }
    }

    fun createStatus(userId: String, username: String, text: String, emoji: String, onSuccess: () -> Unit) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _statusState.update { it.copy(isPosting = true) }
            val result = FirebaseService.createStatus(userId, username, text, emoji)
            _statusState.update { it.copy(isPosting = false) }
            result.onSuccess { onSuccess() }
        }
    }

    fun markStatusAsViewed(statusId: String, userId: String) {
        viewModelScope.launch { FirebaseService.markStatusAsViewed(statusId, userId) }
    }

    // =========================================================
    // CREATE ROOM / DM
    // =========================================================

    fun startPrivateChat(
        currentUserId: String, currentUserName: String,
        targetUser: UserSearchItem, onCreated: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            val fullTarget = FirestoreUser(uid = targetUser.uid, username = targetUser.username)
            val result = FirebaseService.createPrivateChat(currentUserId, currentUserName, fullTarget)
            result.onSuccess { roomId -> onCreated(roomId, targetUser.username) }
        }
    }

    // =========================================================
    // ROOM ACTIONS
    // =========================================================

    fun muteRoom(roomId: String, isPrivate: Boolean, userId: String, muted: Boolean, onDone: () -> Unit) {
        viewModelScope.launch { FirebaseService.muteRoom(roomId, isPrivate, userId, muted); onDone() }
    }

    fun pinRoom(roomId: String, isPrivate: Boolean, userId: String, pinned: Boolean, onDone: () -> Unit) {
        viewModelScope.launch { FirebaseService.pinRoom(roomId, isPrivate, userId, pinned); onDone() }
    }

    fun clearMessages(roomId: String, isPrivate: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            FirebaseService.clearMessages(roomId, isPrivate)
            onDone()
        }
    }

    fun reportBug(userId: String, description: String, roomId: String? = null, onDone: () -> Unit) {
        viewModelScope.launch { FirebaseService.reportBug(userId, description, roomId); onDone() }
    }

    fun createPublicRoom(name: String, emoji: String = "💬", createdBy: String, onCreated: (String) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = FirebaseService.createPublicRoom(name.trim(), createdBy, emoji)
            result.onSuccess { roomId -> onCreated(roomId) }
        }
    }

    // =========================================================
    // NOTIFICAÇÕES
    // =========================================================

    fun startObservingNotifications(userId: String) {
        viewModelScope.launch {
            FirebaseService.observeAllNotifications(userId).collect { list ->
                _notificationState.update {
                    it.copy(
                        notifications = if (list.isEmpty()) UiState.Empty else UiState.Success(list),
                        unreadCount = list.count { n -> !n.isRead }
                    )
                }
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch { FirebaseService.markNotificationAsRead(notificationId) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            notifications.value.filter { !it.isRead }
                .forEach { FirebaseService.markNotificationAsRead(it.id) }
        }
    }
    // Ghost Mode
    fun toggleGhostMode(uid: String, enabled: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result: Result<Unit> = FirebaseService.setGhostMode(uid, enabled)
            result.onSuccess { onDone(enabled) }
        }
    }

    // Self Destruct
    fun sendSelfDestructMessage(
        roomId: String, isPrivate: Boolean, text: String,
        senderId: String, senderName: String,
        destructAfterSeconds: Int
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            FirebaseService.sendSelfDestructMessage(
                roomId, isPrivate, text, senderId, senderName, destructAfterSeconds
            )
        }
    }

    // Verificar e apagar mensagens expiradas

    fun checkAndDeleteExpiredMessages(roomId: String, isPrivate: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val expired = messages.value.filter { msg ->
                msg.selfDestructAt > 0L && msg.selfDestructAt <= now
            }
            expired.forEach { msg ->
                FirebaseService.deleteMessage(roomId, isPrivate, msg.id)
            }
        }
    }

    // Deletar mensagem individual
    fun deleteMessage(roomId: String, isPrivate: Boolean, messageId: String) {
        viewModelScope.launch {
            FirebaseService.deleteMessage(roomId, isPrivate, messageId)
        }
    }
    fun updateProfile(
        uid: String, displayName: String, bio: String,
        avatarEmoji: String = "🐾", photoUrl: String? = null,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _profileState.update { it.copy(isSaving = true) }
            val result = FirebaseService.updateUserProfile(uid, displayName, bio, avatarEmoji, photoUrl)
            _profileState.update { it.copy(isSaving = false, saveSuccess = result.isSuccess) }
            result.onSuccess { onSuccess() }
            result.onFailure { onError(it.message ?: "Erro ao atualizar perfil") }
        }
    }
    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
        typingJob?.cancel()
        typingResetJob?.cancel()
        searchJob?.cancel()
    }
}