package com.andreilima.capychat.data.model

data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val author: String,
    val avatarEmoji: String,
    val unreadCount: Int = 0
)

data class Message(
    val id: String,
    val sender: String,
    val text: String,
    val isMine: Boolean,
    val time: String
)

data class StatusItem(
    val id: String,
    val name: String,
    val text: String,
    val emoji: String,
    val time: String
)