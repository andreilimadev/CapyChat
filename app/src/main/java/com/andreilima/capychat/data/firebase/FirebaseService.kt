package com.andreilima.capychat.data.firebase

import com.andreilima.capychat.data.model.FirestoreMessage
import com.andreilima.capychat.data.model.FirestoreRoom
import com.andreilima.capychat.data.model.FirestoreStatus
import com.andreilima.capychat.data.model.FirestoreUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseService {

    val auth = Firebase.auth
    private val db = Firebase.firestore

    init {
        db.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }
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

            // Gerar tag única: andrei_a8f2
            val randomSuffix = UUID.randomUUID().toString().take(4)
            val userTag = "${username.lowercase().trim()}_$randomSuffix"

            val user = FirestoreUser(
                uid = uid,
                username = username,
                displayName = username,
                searchableUsername = username.lowercase().trim(),
                userTag = userTag,
                email = email
            )

            db.collection("users").document(uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    suspend fun getUser(uid: String): FirestoreUser? {
        return try {
            db.collection("users").document(uid).get().await()
                .toObject(FirestoreUser::class.java)
        } catch (e: Exception) {
            null
        }
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
        senderName: String
    ): Result<Unit> {
        return try {
            val collection = if (isPrivate) "private_rooms" else "public_rooms"
            val message = FirestoreMessage(
                text = text,
                senderId = senderId,
                senderName = senderName,
                timestamp = System.currentTimeMillis()
            )
            val roomRef = db.collection(collection).document(roomId)
            roomRef.collection("messages").add(message)
            roomRef.update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageAt" to System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
        emoji: String
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val status = FirestoreStatus(
                userId = userId,
                username = username,
                text = text,
                emoji = emoji,
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
        } catch (e: Exception) {
            // Silencioso
        }
    }
}
