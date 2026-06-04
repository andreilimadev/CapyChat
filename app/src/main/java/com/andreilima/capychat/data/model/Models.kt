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
    val photoUrl: String? = null,
    val unreadCount: Int = 0,
    val lastTime: String = "",
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isPrivate: Boolean = false,
    val isOnline: Boolean = false
)

data class Message(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val isMine: Boolean = false,
    val time: String = "",
    val messageType: String = "text",
    val status: MessageStatus = MessageStatus.SENT,
    val reactions: Map<String, String> = emptyMap(), // ← NOVO: userId -> emoji
    val replyToText: String = "",                    // ← NOVO: texto da msg original
    val replyToSender: String = ""                   // ← NOVO: nome do remetente original
)


enum class MessageStatus {
    SENT,       // ✓
    DELIVERED,  // ✓✓
    READ        // ✓✓ azul
}

data class StatusItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val text: String = "",
    val emoji: String = "🦫",
    val photoUrl: String? = null,
    val time: String = "",
    val isNew: Boolean = true
)

data class UserSearchItem(
    val uid: String,
    val username: String,
    val userTag: String,
    val photoUrl: String? = null,
    val isOnline: Boolean = false
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
    val avatarEmoji: String = "🐾",   // ← NOVO
    val bio: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val fcmToken: String = ""
)

data class FirestoreRoom(
    val name: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val avatarEmoji: String = "💬",
    val createdBy: String = "",
    val isPrivate: Boolean = false,
    val participants: Map<String, Boolean> = emptyMap(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantEmojis: Map<String, String> = emptyMap(), // ← NOVO
    val typingUsers: Map<String, Boolean> = emptyMap(),
    val pinnedBy: Map<String, Boolean> = emptyMap(),         // ← NOVO
    val mutedBy: Map<String, Boolean> = emptyMap()           // ← NOVO
)

data class FirestoreMessage(
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0L,
    val messageType: String = "text",
    val readBy: Map<String, Boolean> = emptyMap(),
    val deliveredTo: Map<String, Boolean> = emptyMap(),
    val reactions: Map<String, String> = emptyMap(), // ← NOVO
    val replyToText: String = "",                    // ← NOVO
    val replyToSender: String = "",                  // ← NOVO
    val selfDestructAt: Long = 0L,
    val isGhost: Boolean = false
)
data class FirestoreStatus(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userTag: String = "",
    val text: String = "",
    val emoji: String = "🦫",
    val photoUrl: String? = null,                           // NOVO
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L,
    val viewedBy: Map<String, Boolean> = emptyMap()
)

// NOVO — Notificações
data class FirestoreNotification(
    val id: String = "",
    val toUserId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val type: String = "",      // message | status | mention
    val payload: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
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

    val status = when {
        readBy.containsKey(currentUserId) -> MessageStatus.READ
        deliveredTo.containsKey(currentUserId) -> MessageStatus.DELIVERED
        else -> MessageStatus.SENT
    }

    return Message(
        id = id,
        sender = senderName,
        text = text,
        isMine = senderId == currentUserId,
        time = timeStr,
        messageType = messageType,
        status = status,
        reactions = reactions,
        replyToText = replyToText,
        replyToSender = replyToSender,
        selfDestructAt = selfDestructAt  // ← FASE 5
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
        photoUrl = photoUrl,
        time = timeStr,
        isNew = !viewedBy.containsKey(currentUserId) && userId != currentUserId
    )
}

fun FirestoreUser.toSearchItem(): UserSearchItem = UserSearchItem(
    uid = uid,
    username = username,
    userTag = userTag,
    photoUrl = photoUrl,
    isOnline = isOnline,

)