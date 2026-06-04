package com.andreilima.capychat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andreilima.capychat.data.firebase.FirebaseService
import com.andreilima.capychat.data.model.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    // =========================================================
    // STATE
    // =========================================================

    private val _rooms = MutableStateFlow<List<ChatItem>>(emptyList())
    val rooms: StateFlow<List<ChatItem>> = _rooms.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _statuses = MutableStateFlow<List<StatusItem>>(emptyList())
    val statuses: StateFlow<List<StatusItem>> = _statuses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserSearchItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<FirestoreUser?>(null)
    val currentUserProfile: StateFlow<FirestoreUser?> = _currentUserProfile.asStateFlow()

    private val _unreadNotificationsCount = MutableStateFlow(0)
    private val _notifications = MutableStateFlow<List<FirestoreNotification>>(emptyList())
    val notifications: StateFlow<List<FirestoreNotification>> = _notifications.asStateFlow()
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()

    private var currentRoomId: String? = null
    private var currentRoomIsPrivate: Boolean = false
    private var currentUserId: String? = null

    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var typingResetJob: Job? = null
    private var searchJob: Job? = null

    init {
        startSearchQueryObservation()
    }

    // =========================================================
    // USER SEARCH
    // =========================================================

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    @OptIn(FlowPreview::class)
    private fun startSearchQueryObservation() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 3) performSearch(query)
                    else _searchResults.value = emptyList()
                }
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            val users = FirebaseService.searchUsers(query)
            _searchResults.value = users.map { it.toSearchItem() }
            _isSearching.value = false
        }
    }

    // =========================================================
    // PERFIL
    // =========================================================

    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            FirebaseService.observeUser(uid).collect { user ->
                _currentUserProfile.value = user
            }
        }
    }

    fun updateProfile(
        uid: String,
        displayName: String,
        bio: String,
        avatarEmoji: String = "🐾",
        photoUrl: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = FirebaseService.updateUserProfile(uid, displayName, bio, avatarEmoji, photoUrl)
            _isLoading.value = false
            result.onSuccess { onSuccess() }
            result.onFailure { onError(it.message ?: "Erro ao atualizar perfil") }
        }
    }

    // =========================================================
    // STATUS ONLINE
    // =========================================================

    fun setOnline(uid: String) {
        viewModelScope.launch { FirebaseService.setOnlineStatus(uid, true) }
    }

    fun setOffline(uid: String) {
        viewModelScope.launch { FirebaseService.setOnlineStatus(uid, false) }
    }

    // =========================================================
    // LOGOUT — ✅ CORRIGIDO: agora aguarda o status ser salvo
    // =========================================================

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            FirebaseService.logout()
            onDone()
        }
    }

    // =========================================================
    // ROOMS
    // =========================================================

    fun startObservingRooms(uid: String) {
        currentUserId = uid
        viewModelScope.launch {
            FirebaseService.observeRooms(uid).collect { pairs ->
                _rooms.value = pairs.map { (id, room) -> room.toChatItem(id, uid) }
            }
        }
    }

    // =========================================================
    // MESSAGES
    // =========================================================

    fun startObservingMessages(
        roomId: String,
        isPrivate: Boolean,
        currentUserId: String
    ) {
        this.currentRoomId = roomId
        this.currentRoomIsPrivate = isPrivate
        this.currentUserId = currentUserId

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            FirebaseService.observeMessages(roomId, isPrivate)
                .collect { pairs ->
                    _messages.value = pairs.map { (id, msg) ->
                        msg.toMessage(id, currentUserId)
                    }
                    // ✅ CORRIGIDO: marca TODAS as mensagens não lidas (não só as últimas 5)
                    pairs.forEach { (id, msg) ->
                        if (msg.senderId != currentUserId && !msg.readBy.containsKey(currentUserId)) {
                            FirebaseService.markMessageAsRead(roomId, isPrivate, id, currentUserId)
                        }
                    }
                }
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            FirebaseService.observeTyping(roomId, isPrivate, currentUserId)
                .collect { _typingUsers.value = it }
        }
    }

    fun stopObservingMessages() {
        currentRoomId?.let { roomId ->
            currentUserId?.let { uid ->
                viewModelScope.launch {
                    FirebaseService.setTyping(roomId, currentRoomIsPrivate, uid, false)
                }
            }
        }
        messagesJob?.cancel()
        typingJob?.cancel()
        typingResetJob?.cancel()
        _messages.value = emptyList()
        _typingUsers.value = emptyList()
        currentRoomId = null
    }

    // =========================================================
    // DIGITANDO
    // =========================================================

    fun onUserTyping(roomId: String, isPrivate: Boolean, userId: String) {
        viewModelScope.launch {
            FirebaseService.setTyping(roomId, isPrivate, userId, true)
        }
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
        replyToText: String = "",
        replyToSender: String = ""
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            FirebaseService.setTyping(roomId, isPrivate, senderId, false)
            typingResetJob?.cancel()
            FirebaseService.sendMessage(
                roomId = roomId,
                isPrivate = isPrivate,
                text = text.trim(),
                senderId = senderId,
                senderName = senderName,
                replyToText = replyToText,
                replyToSender = replyToSender
            )
        }
    }

    // =========================================================
    // REACTIONS — ✅ CORRIGIDO: passa currentEmoji para toggle
    // =========================================================

    fun reactToMessage(
        roomId: String,
        isPrivate: Boolean,
        messageId: String,
        userId: String,
        emoji: String,
        currentEmoji: String? = null
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
            FirebaseService.observeStatuses().collect { list ->
                _statuses.value = list.map { it.toStatusItem(currentUserId) }
            }
        }
    }

    fun createStatus(
        userId: String,
        username: String,
        text: String,
        emoji: String,
        onSuccess: () -> Unit
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = FirebaseService.createStatus(userId, username, text, emoji)
            _isLoading.value = false
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
        currentUserId: String,
        currentUserName: String,
        targetUser: UserSearchItem,
        onCreated: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val fullTarget = FirestoreUser(uid = targetUser.uid, username = targetUser.username)
            val result = FirebaseService.createPrivateChat(currentUserId, currentUserName, fullTarget)
            _isLoading.value = false
            result.onSuccess { roomId -> onCreated(roomId, targetUser.username) }
        }
    }

    // =========================================================
    // ROOM ACTIONS
    // =========================================================

    fun muteRoom(roomId: String, isPrivate: Boolean, userId: String, muted: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            FirebaseService.muteRoom(roomId, isPrivate, userId, muted)
            onDone()
        }
    }

    fun pinRoom(roomId: String, isPrivate: Boolean, userId: String, pinned: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            FirebaseService.pinRoom(roomId, isPrivate, userId, pinned)
            onDone()
        }
    }

    fun clearMessages(roomId: String, isPrivate: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            FirebaseService.clearMessages(roomId, isPrivate)
            _isLoading.value = false
            onDone()
        }
    }

    fun reportBug(userId: String, description: String, roomId: String? = null, onDone: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            FirebaseService.reportBug(userId, description, roomId)
            _isLoading.value = false
            onDone()
        }
    }

    fun createPublicRoom(name: String, emoji: String = "💬", createdBy: String, onCreated: (String) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = FirebaseService.createPublicRoom(name.trim(), createdBy, emoji)
            _isLoading.value = false
            result.onSuccess { roomId -> onCreated(roomId) }
        }
    }

    // =========================================================
    // NOTIFICAÇÕES
    // =========================================================

    fun startObservingNotifications(userId: String) {
        viewModelScope.launch {
            FirebaseService.observeAllNotifications(userId).collect { list ->
                _notifications.value = list
                _unreadNotificationsCount.value = list.count { !it.isRead }
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch { FirebaseService.markNotificationAsRead(notificationId) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            _notifications.value
                .filter { !it.isRead }
                .forEach { FirebaseService.markNotificationAsRead(it.id) }
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