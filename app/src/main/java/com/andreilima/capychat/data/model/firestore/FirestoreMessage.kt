package com.andreilima.capychat.data.model.firestore

data class FirestoreMessage(
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0L,
    val messageType: String = "text",
    val readBy: Map<String, Boolean> = emptyMap(),
    val deliveredTo: Map<String, Boolean> = emptyMap(),
    val reactions: Map<String, String> = emptyMap(),
    val replyToText: String = "",
    val replyToSender: String = "",
    val selfDestructAt: Long = 0L,   // ← FASE 5
    val isGhost: Boolean = false     // ← FASE 5
)

