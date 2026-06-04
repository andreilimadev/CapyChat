package com.andreilima.capychat.ui.state

import com.andreilima.capychat.data.model.Message
import com.andreilima.capychat.data.model.ChatItem
import com.andreilima.capychat.data.model.StatusItem
import com.andreilima.capychat.data.model.FirestoreNotification
import com.andreilima.capychat.data.model.UserSearchItem
import com.andreilima.capychat.data.model.FirestoreUser

data class ConversationUiState(
    val rooms: UiState<List<ChatItem>> = UiState.Loading,
    val searchQuery: String = "",
    val searchResults: List<UserSearchItem> = emptyList(),
    val isSearching: Boolean = false
)

data class ChatUiState(
    val messages: UiState<List<Message>> = UiState.Loading,
    val typingUsers: List<String> = emptyList(),
    val replyingTo: Message? = null,
    val searchQuery: String = "",
    val isSending: Boolean = false
)

data class ProfileUiState(
    val user: UiState<FirestoreUser> = UiState.Loading,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

data class StatusUiState(
    val statuses: UiState<List<StatusItem>> = UiState.Loading,
    val isPosting: Boolean = false
)

data class NotificationUiState(
    val notifications: UiState<List<FirestoreNotification>> = UiState.Loading,
    val unreadCount: Int = 0
)