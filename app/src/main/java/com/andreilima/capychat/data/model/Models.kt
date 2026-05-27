package com.andreilima.capychat.data.model

import java.util.Calendar

// =========================================================
// UI MODELS
// =========================================================

data class ChatItem(
    val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val author: String = "",
    val avatarEmoji: String = "💬",
    val unreadCount: Int = 0,
    val lastTime: String = "",
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isPrivate: Boolean = false
)

data class Message(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val isMine: Boolean = false,
    val time: String = ""
)

data class StatusItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val text: String = "",
    val emoji: String = "🦫",
    val time: String = "",
    val isNew: Boolean = true
)

data class UserSearchItem(
    val uid: String,
    val username: String,
    val userTag: String,
    val photoUrl: String? = null
)

// =========================================================
// FIREBASE MODELS
// =========================================================

data class FirestoreUser(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val searchableUsername: String = "",
    val userTag: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class FirestoreRoom(
    val name: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val avatarEmoji: String = "💬",
    val createdBy: String = "",
    val isPrivate: Boolean = false,
    val participants: Map<String, Boolean> = emptyMap(),
    val participantNames: Map<String, String> = emptyMap()
)

data class FirestoreMessage(
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0L
)

data class FirestoreStatus(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userTag: String = "",
    val text: String = "",
    val emoji: String = "🦫",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L,
    val viewedBy: Map<String, Boolean> = emptyMap()
)

// =========================================================
// CONVERSORES
// =========================================================

fun FirestoreRoom.toChatItem(id: String, currentUserId: String): ChatItem {
    val timeStr = if (lastMessageAt > 0) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = lastMessageAt
        String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    } else ""

    val finalName = if (isPrivate) {
        participantNames.filterKeys { it != currentUserId }.values.firstOrNull() ?: name
    } else {
        name
    }

    return ChatItem(
        id = id,
        name = finalName,
        lastMessage = lastMessage,
        author = "", 
        avatarEmoji = avatarEmoji,
        lastTime = timeStr,
        isPrivate = isPrivate
    )
}

fun FirestoreMessage.toMessage(id: String, currentUserId: String): Message {
    val timeStr = if (timestamp > 0) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    } else "Agora"

    return Message(
        id = id,
        sender = senderName,
        text = text,
        isMine = senderId == currentUserId,
        time = timeStr
    )
}

fun FirestoreStatus.toStatusItem(currentUserId: String): StatusItem {
    val diff = System.currentTimeMillis() - createdAt
    val timeStr = when {
        diff < 60_000 -> "agora"
        diff < 3600_000 -> "há ${diff / 60_000}m"
        else -> "há ${diff / 3600_000}h"
    }

    return StatusItem(
        id = id,
        userId = userId,
        name = username,
        text = text,
        emoji = emoji,
        time = timeStr,
        isNew = !viewedBy.containsKey(currentUserId) && userId != currentUserId
    )
}

fun FirestoreUser.toSearchItem(): UserSearchItem = UserSearchItem(
    uid = uid,
    username = username,
    userTag = userTag,
    photoUrl = photoUrl
)
