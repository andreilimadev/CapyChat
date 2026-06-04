package com.andreilima.capychat.data.firebase

import com.andreilima.capychat.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

// FirebaseService agora delega para repositórios especializados.
// Mantido para compatibilidade com ChatViewModel durante a migração.
object FirebaseService {
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()


    fun initFirestore() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    // AUTH
    suspend fun login(email: String, password: String) = AuthRepository.login(email, password)
    suspend fun register(username: String, email: String, password: String) = AuthRepository.register(username, email, password)
    suspend fun logout() = AuthRepository.logout()

    // USER
    suspend fun getUser(uid: String) = UserRepository.getUser(uid)
    suspend fun updateUserProfile(uid: String, displayName: String, bio: String, avatarEmoji: String = "🐾", photoUrl: String? = null) =
        UserRepository.updateUserProfile(uid, displayName, bio, avatarEmoji, photoUrl)
    suspend fun setOnlineStatus(uid: String, isOnline: Boolean) = UserRepository.setOnlineStatus(uid, isOnline)
    fun observeUser(uid: String) = UserRepository.observeUser(uid)
    suspend fun searchUsers(query: String) = UserRepository.searchUsers(query)

    // ROOMS & CHAT
    fun observeRooms(uid: String) = ChatRepository.observeRooms(uid)
    suspend fun createPublicRoom(name: String, createdBy: String, emoji: String = "💬") = ChatRepository.createPublicRoom(name, createdBy, emoji)
    suspend fun createPrivateChat(currentUserId: String, currentUserName: String, targetUser: FirestoreUser) =
        ChatRepository.createPrivateChat(currentUserId, currentUserName, targetUser)
    fun observeMessages(roomId: String, isPrivate: Boolean) = ChatRepository.observeMessages(roomId, isPrivate)
    suspend fun sendMessage(
        roomId: String,
        isPrivate: Boolean,
        text: String,
        senderId: String,
        senderName: String,
        messageType: String = "text",
        replyToText: String = "",
        replyToSender: String = "",
        selfDestructAt: Long = 0L      // ← FASE 5
    ): Result<Unit> {
        return try {
            val collection = if (isPrivate) "private_rooms" else "public_rooms"
            val message = FirestoreMessage(
                text = text,
                senderId = senderId,
                senderName = senderName,
                timestamp = System.currentTimeMillis(),
                messageType = messageType,
                deliveredTo = mapOf(senderId to true),
                replyToText = replyToText,
                replyToSender = replyToSender,
                selfDestructAt = selfDestructAt   // ← FASE 5
            )
            val roomRef = db.collection(collection).document(roomId)
            roomRef.collection("messages").add(message).await()
            roomRef.update(
                mapOf(
                    "lastMessage" to if (messageType == "selfdestruct") "💣 Mensagem autodestrutiva" else text,
                    "lastMessageAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun markMessageAsRead(roomId: String, isPrivate: Boolean, messageId: String, userId: String) =
        ChatRepository.markMessageAsRead(roomId, isPrivate, messageId, userId)
    suspend fun reactToMessage(roomId: String, isPrivate: Boolean, messageId: String, userId: String, emoji: String, currentEmoji: String? = null) =
        ChatRepository.reactToMessage(roomId, isPrivate, messageId, userId, emoji, currentEmoji)
    suspend fun muteRoom(roomId: String, isPrivate: Boolean, userId: String, muted: Boolean) = ChatRepository.muteRoom(roomId, isPrivate, userId, muted)
    suspend fun pinRoom(roomId: String, isPrivate: Boolean, userId: String, pinned: Boolean) = ChatRepository.pinRoom(roomId, isPrivate, userId, pinned)
    suspend fun clearMessages(roomId: String, isPrivate: Boolean) = ChatRepository.clearMessages(roomId, isPrivate)
    suspend fun reportBug(userId: String, description: String, roomId: String? = null) = ChatRepository.reportBug(userId, description, roomId)
    suspend fun setTyping(roomId: String, isPrivate: Boolean, userId: String, isTyping: Boolean) = ChatRepository.setTyping(roomId, isPrivate, userId, isTyping)
    fun observeTyping(roomId: String, isPrivate: Boolean, currentUserId: String) = ChatRepository.observeTyping(roomId, isPrivate, currentUserId)

    // STATUS
    fun observeStatuses() = StatusRepository.observeStatuses()
    suspend fun createStatus(userId: String, username: String, text: String, emoji: String, photoUrl: String? = null) =
        StatusRepository.createStatus(userId, username, text, emoji, photoUrl)
    suspend fun markStatusAsViewed(statusId: String, userId: String) = StatusRepository.markStatusAsViewed(statusId, userId)

    // NOTIFICATIONS
    suspend fun createNotification(notification: FirestoreNotification) = NotificationRepository.createNotification(notification)
    fun observeAllNotifications(userId: String) = NotificationRepository.observeAllNotifications(userId)
    suspend fun markNotificationAsRead(notificationId: String) = NotificationRepository.markNotificationAsRead(notificationId)
    // MEDIA
    fun uploadFile(uri: android.net.Uri, path: String) = MediaRepository.uploadFile(uri, path)
    suspend fun deleteFile(downloadUrl: String) = MediaRepository.deleteFile(downloadUrl)
    suspend fun sendSelfDestructMessage(roomId: String, isPrivate: Boolean, text: String, senderId: String, senderName: String, destructAfterSeconds: Int) =
        ChatRepository.sendSelfDestructMessage(roomId, isPrivate, text, senderId, senderName, destructAfterSeconds)


    suspend fun deleteMessage(roomId: String, isPrivate: Boolean, messageId: String) {
        try {
            val collection = if (isPrivate) "private_rooms" else "public_rooms"
            db.collection(collection).document(roomId)
                .collection("messages").document(messageId)
                .delete().await()
        } catch (e: Exception) { }
    }
    suspend fun setGhostMode(uid: String, enabled: Boolean): Result<Unit> {
        return try {
            db.collection("users").document(uid).update(
                mapOf("isGhostMode" to enabled, "isOnline" to !enabled)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}