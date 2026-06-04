package com.andreilima.capychat.data.model.ui

data class ChatItem(
    val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val lastTime: String = "",
    val unreadCount: Int = 0,
    val avatarEmoji: String = "🐾",
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)