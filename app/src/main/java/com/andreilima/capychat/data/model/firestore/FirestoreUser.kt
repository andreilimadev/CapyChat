package com.andreilima.capychat.data.model.firestore

data class FirestoreUser(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val searchableUsername: String = "",
    val userTag: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val avatarEmoji: String = "🐾",
    val bio: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val fcmToken: String = "",       // ← FASE 4
    val isGhostMode: Boolean = false // ← FASE 5
)
