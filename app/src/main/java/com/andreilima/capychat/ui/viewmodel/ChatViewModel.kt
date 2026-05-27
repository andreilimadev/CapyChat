package com.andreilima.capychat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andreilima.capychat.data.firebase.FirebaseService
import com.andreilima.capychat.data.model.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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

    // Buscas
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserSearchItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private var messagesJob: Job? = null
    private var searchJob: Job? = null

    @OptIn(FlowPreview::class)
    fun startSearchQueryObservation() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 3) {
                        performSearch(query)
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }
    }

    init {
        startSearchQueryObservation()
    }

    // =========================================================
    // USER SEARCH
    // =========================================================

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
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
    // ROOMS
    // =========================================================

    fun startObservingRooms(uid: String) {
        viewModelScope.launch {
            FirebaseService.observeRooms(uid).collect { pairs ->
                _rooms.value = pairs.map { (id, room) ->
                    room.toChatItem(id, uid)
                }
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
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            FirebaseService.observeMessages(roomId, isPrivate)
                .collect { pairs ->
                    _messages.value = pairs.map { (id, msg) ->
                        msg.toMessage(id, currentUserId)
                    }
                }
        }
    }

    fun stopObservingMessages() {
        messagesJob?.cancel()
        _messages.value = emptyList()
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
        viewModelScope.launch {
            FirebaseService.markStatusAsViewed(statusId, userId)
        }
    }

    fun sendMessage(
        roomId: String,
        isPrivate: Boolean,
        text: String,
        senderId: String,
        senderName: String
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            FirebaseService.sendMessage(
                roomId = roomId,
                isPrivate = isPrivate,
                text = text.trim(),
                senderId = senderId,
                senderName = senderName
            )
        }
    }

    // =========================================================
    // CREATE / DM
    // =========================================================

    fun startPrivateChat(
        currentUserId: String,
        currentUserName: String,
        targetUser: UserSearchItem,
        onCreated: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            // Criar um FirestoreUser mínimo para a função do FirebaseService
            val fullTarget = FirestoreUser(
                uid = targetUser.uid,
                username = targetUser.username
            )
            
            val result = FirebaseService.createPrivateChat(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                targetUser = fullTarget
            )
            
            _isLoading.value = false
            result.onSuccess { roomId ->
                onCreated(roomId, targetUser.username)
            }
        }
    }

    fun createPublicRoom(
        name: String,
        emoji: String = "💬",
        createdBy: String,
        onCreated: (String) -> Unit
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = FirebaseService.createPublicRoom(
                name = name.trim(),
                createdBy = createdBy,
                emoji = emoji
            )
            _isLoading.value = false
            result.onSuccess { roomId ->
                onCreated(roomId)
            }
        }
    }
}
