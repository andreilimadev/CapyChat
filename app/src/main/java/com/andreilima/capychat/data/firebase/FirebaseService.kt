package com.andreilima.capychat.data.firebase

import com.andreilima.capychat.data.model.FirestoreMessage
import com.andreilima.capychat.data.model.FirestoreNotification
import com.andreilima.capychat.data.model.FirestoreRoom
import com.andreilima.capychat.data.model.FirestoreStatus
import com.andreilima.capychat.data.model.FirestoreUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseService {

    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    fun initFirestore() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    // =========================================================
    // AUTH & USER
    // =========================================================

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: error("UID nulo")
            val randomSuffix = UUID.randomUUID().toString().take(4)
            val userTag = "${username.lowercase().trim()}_$randomSuffix"

            val user = FirestoreUser(
                uid = uid,
                username = username,
                displayName = username,
                searchableUsername = username.lowercase().trim(),
                userTag = userTag,
                email = email,
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
            db.collection("users").document(uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to false,
                    "lastSeen" to System.currentTimeMillis()
                )
            )
        }
        auth.signOut()
    }

    suspend fun getUser(uid: String): FirestoreUser? {
        return try {
            db.collection("users").document(uid).get().await()
                .toObject(FirestoreUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(
        uid: String,
        displayName: String,
        bio: String,
        photoUrl: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName,
                "bio" to bio
            )
            if (photoUrl != null) updates["photoUrl"] = photoUrl
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setOnlineStatus(uid: String, isOnline: Boolean) {
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            // Silencioso
        }
    }

    fun observeUser(uid: String): Flow<FirestoreUser?> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(FirestoreUser::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun searchUsers(query: String): List<FirestoreUser> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = query.lowercase().trim()
        return try {
            db.collection("users")
                .orderBy("searchableUsername")
                .startAt(normalizedQuery)
                .endAt(normalizedQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .toObjects(FirestoreUser::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // =========================================================
    // ROOMS
    // =========================================================

    fun observeRooms(uid: String): Flow<List<Pair<String, FirestoreRoom>>> {
        val publicFlow = callbackFlow {
            val listener = db.collection("public_rooms")
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    val rooms = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(FirestoreRoom::class.java)?.let { Pair(doc.id, it) }
                    } ?: emptyList()
                    trySend(rooms)
                }
            awaitClose { listener.remove() }
        }

        val privateFlow = callbackFlow {
            val listener = db.collection("private_rooms")
                .whereEqualTo("participants.$uid", true)
                .addSnapshotListener { snapshot, _ ->
                    val rooms = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(FirestoreRoom::class.java)?.let { Pair(doc.id, it) }
                    } ?: emptyList()
                    trySend(rooms)
                }
            awaitClose { listener.remove() }
        }

        return combine(publicFlow, privateFlow) { public, private ->
            (public + private).sortedByDescending { it.second.lastMessageAt }
        }
    }

    suspend fun createPublicRoom(
        name: String,
        createdBy: String,
        emoji: String = "💬"
    ): Result<String> {
        return try {
            val room = FirestoreRoom(
                name = name,
                lastMessage = "Sala criada",
                lastMessageAt = System.currentTimeMillis(),
                avatarEmoji = emoji,
                createdBy = createdBy,
                isPrivate = false
            )
            val ref = db.collection("public_rooms").add(room).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPrivateChat(
        currentUserId: String,
        currentUserName: String,
        targetUser: FirestoreUser
    ): Result<String> {
        if (currentUserId == targetUser.uid) {
            return Result.failure(Exception("Não pode conversar consigo mesmo"))
        }

        val roomId = listOf(currentUserId, targetUser.uid).sorted().joinToString("_")
        val roomRef = db.collection("private_rooms").document(roomId)

        return try {
            val existing = roomRef.get().await()
            if (existing.exists()) {
                Result.success(roomId)
            } else {
                val room = FirestoreRoom(
                    name = "Chat Privado",
                    lastMessage = "Conversa iniciada",
                    lastMessageAt = System.currentTimeMillis(),
                    avatarEmoji = "🔒",
                    createdBy = currentUserId,
                    isPrivate = true,
                    participants = mapOf(currentUserId to true, targetUser.uid to true),
                    participantNames = mapOf(
                        currentUserId to currentUserName,
                        targetUser.uid to targetUser.username
                    )
                )
                roomRef.set(room).await()
                Result.success(roomId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // =========================================================
// ROOM PREFERENCES (Silenciar / Fixar / Limpar)
// =========================================================

    suspend fun muteRoom(
        roomId: String,
        isPrivate: Boolean,
        userId: String,
        muted: Boolean
    ): Result<Unit> {
        return try {
            val collection = if (isPrivate) "private_rooms" else "public_rooms"
            db.collection(collection).document(roomId)
                .update("mutedBy.$userId", muted)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pinRoom(
        roomId: String,
        isPrivate: Boolean,
        userId: String,
        pinned: Boolean
    ): Result<Unit> {
        return try {
            val collection = if (isPrivate) "private_rooms" else "public_rooms"
            db.collection(collection).document(roomId)
                .update("pinnedBy.$userId", pinned)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearMessages(
        roomId: String,
        isPrivate: Boolean
    ): Result<Unit> {
        return try {
            val collection = if (isPrivate) "private_rooms" else "public_rooms"
            val messagesRef = db.collection(collection).document(roomId).collection("messages")
            val batch = db.batch()
            val docs = messagesRef.get().await()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportBug(
        userId: String,
        description: String,
        roomId: String? = null
    ): Result<Unit> {
        return try {
            val report = mapOf(
                "userId" to userId,
                "description" to description,
                "roomId" to (roomId ?: ""),
                "timestamp" to System.currentTimeMillis(),
                "appVersion" to "1.0",
                "resolved" to false
            )
            db.collection("bug_reports").add(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================
    // TYPING INDICATOR
    // =========================================================

    suspend fun setTyping(roomId: String, isPrivate: Boolean, userId: String, isTyping: Boolean) {
        try {
            val collection = if (isPrivate) "private_rooms" else "public_rooms"
            db.collection(collection).document(roomId)
                .update("typingUsers.$userId", isTyping)
                .await()
        } catch (e: Exception) {
            // Silencioso
        }
    }

    fun observeTyping(roomId: String, isPrivate: Boolean, currentUserId: String): Flow<List<String>> = callbackFlow {
        val collection = if (isPrivate) "private_rooms" else "public_rooms"
        val listener = db.collection(collection).document(roomId)
            .addSnapshotListener { snapshot, _ ->
                val typingUsers = snapshot?.get("typingUsers") as? Map<*, *>
                val typing = typingUsers
                    ?.filter { (k, v) -> k != currentUserId && v == true }
                    ?.keys
                    ?.mapNotNull { it as? String }
                    ?: emptyList()
                trySend(typing)
            }
        awaitClose { listener.remove() }
    }

    // =========================================================
    // MESSAGES
    // =========================================================

    fun observeMessages(
        roomId: String,
        isPrivate: Boolean
    ): Flow<List<Pair<String, FirestoreMessage>>> = callbackFlow {
        val collection = if (isPrivate) "private_rooms" else "public_rooms"
        val listener = db.collection(collection)
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirestoreMessage::class.java)?.let { Pair(doc.id, it) }
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        roomId: String,
        isPrivate: Boolean,
        text: String,
        senderId: String,
        senderName: String,
        messageType: String = "text"
    ): Result<Unit> {
        return try {
            val collection = if (isPrivate) "private_rooms" else "public_rooms"
            val message = FirestoreMessage(
                text = text,
                senderId = senderId,
                senderName = senderName,
                timestamp = System.currentTimeMillis(),
                messageType = messageType,
                deliveredTo = mapOf(senderId to true)
            )
            val roomRef = db.collection(collection).document(roomId)
            roomRef.collection("messages").add(message).await()
            roomRef.update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessageAsRead(
        roomId: String,
        isPrivate: Boolean,
        messageId: String,
        userId: String
    ) {
        try {
            val collection = if (isPrivate) "private_rooms" else "public_rooms"
            db.collection(collection).document(roomId)
                .collection("messages").document(messageId)
                .update("readBy.$userId", true)
                .await()
        } catch (e: Exception) {
            // Silencioso
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    fun observeStatuses(): Flow<List<FirestoreStatus>> = callbackFlow {
        val listener = db.collection("statuses")
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirestoreStatus::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    suspend fun createStatus(
        userId: String,
        username: String,
        text: String,
        emoji: String,
        photoUrl: String? = null
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val status = FirestoreStatus(
                userId = userId,
                username = username,
                text = text,
                emoji = emoji,
                photoUrl = photoUrl,
                createdAt = now,
                expiresAt = now + (24 * 60 * 60 * 1000)
            )
            db.collection("statuses").add(status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markStatusAsViewed(statusId: String, userId: String) {
        try {
            db.collection("statuses").document(statusId)
                .update("viewedBy.$userId", true)
                .await()
        } catch (e: Exception) {
            // Silencioso
        }
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    suspend fun createNotification(notification: FirestoreNotification): Result<Unit> {
        return try {
            db.collection("notifications").add(notification).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<FirestoreNotification>> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("isRead", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirestoreNotification::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            db.collection("notifications").document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            // Silencioso
        }
    }
}