package com.andreilima.capychat.data.firebase

import com.andreilima.capychat.data.model.FirestoreMessage
import com.andreilima.capychat.data.model.FirestoreRoom
import com.andreilima.capychat.data.model.FirestoreUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

object ChatRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private fun collection(isPrivate: Boolean) =
        if (isPrivate) "private_rooms" else "public_rooms"

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
        return combine(publicFlow, privateFlow) { pub, priv ->
            (pub + priv).sortedByDescending { it.second.lastMessageAt }
        }
    }

    suspend fun createPublicRoom(name: String, createdBy: String, emoji: String = "💬"): Result<String> {
        return try {
            val room = FirestoreRoom(
                name = name, lastMessage = "Sala criada",
                lastMessageAt = System.currentTimeMillis(),
                avatarEmoji = emoji, createdBy = createdBy, isPrivate = false
            )
            val ref = db.collection("public_rooms").add(room).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createPrivateChat(
        currentUserId: String, currentUserName: String, targetUser: FirestoreUser
    ): Result<String> {
        if (currentUserId == targetUser.uid)
            return Result.failure(Exception("Não pode conversar consigo mesmo"))
        val roomId = listOf(currentUserId, targetUser.uid).sorted().joinToString("_")
        val roomRef = db.collection("private_rooms").document(roomId)
        return try {
            if (roomRef.get().await().exists()) {
                Result.success(roomId)
            } else {
                val room = FirestoreRoom(
                    name = "Chat Privado",
                    lastMessage = "Conversa iniciada",
                    lastMessageAt = System.currentTimeMillis(),
                    avatarEmoji = "🔒", createdBy = currentUserId, isPrivate = true,
                    participants = mapOf(currentUserId to true, targetUser.uid to true),
                    participantNames = mapOf(currentUserId to currentUserName, targetUser.uid to targetUser.username)
                )
                roomRef.set(room).await()
                Result.success(roomId)
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    fun observeMessages(roomId: String, isPrivate: Boolean): Flow<List<Pair<String, FirestoreMessage>>> = callbackFlow {
        val listener = db.collection(collection(isPrivate)).document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirestoreMessage::class.java)?.let { Pair(doc.id, it) }
                } ?: emptyList()
                trySend(msgs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        roomId: String, isPrivate: Boolean, text: String,
        senderId: String, senderName: String,
        messageType: String = "text", replyToText: String = "", replyToSender: String = ""
    ): Result<Unit> {
        return try {
            val message = FirestoreMessage(
                text = text, senderId = senderId, senderName = senderName,
                timestamp = System.currentTimeMillis(), messageType = messageType,
                deliveredTo = mapOf(senderId to true),
                replyToText = replyToText, replyToSender = replyToSender
            )
            val roomRef = db.collection(collection(isPrivate)).document(roomId)

            // Salva a mensagem
            roomRef.collection("messages").add(message).await()

            // Busca os participantes para incrementar unreadCount de cada um (exceto remetente)
            val roomSnap = roomRef.get().await()
            val room = roomSnap.toObject(FirestoreRoom::class.java)
            val updates = mutableMapOf<String, Any>(
                "lastMessage" to text,
                "lastMessageAt" to System.currentTimeMillis()
            )
            room?.participants?.keys
                ?.filter { it != senderId }
                ?.forEach { uid ->
                    val current = room.unreadCount[uid] ?: 0
                    updates["unreadCount.$uid"] = current + 1
                }

            roomRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun markMessageAsRead(roomId: String, isPrivate: Boolean, messageId: String, userId: String) {
        try {
            val roomRef = db.collection(collection(isPrivate)).document(roomId)
            roomRef.collection("messages").document(messageId)
                .update("readBy.$userId", true).await()
            roomRef.update("unreadCount.$userId", 0).await()
        } catch (e: Exception) { }
    }

    suspend fun reactToMessage(
        roomId: String, isPrivate: Boolean, messageId: String,
        userId: String, emoji: String, currentEmoji: String? = null
    ) {
        try {
            val docRef = db.collection(collection(isPrivate)).document(roomId)
                .collection("messages").document(messageId)
            if (currentEmoji == emoji)
                docRef.update("reactions.$userId", FieldValue.delete()).await()
            else
                docRef.update("reactions.$userId", emoji).await()
        } catch (e: Exception) { }
    }

    suspend fun muteRoom(roomId: String, isPrivate: Boolean, userId: String, muted: Boolean): Result<Unit> {
        return try {
            db.collection(collection(isPrivate)).document(roomId)
                .update("mutedBy.$userId", muted).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun pinRoom(roomId: String, isPrivate: Boolean, userId: String, pinned: Boolean): Result<Unit> {
        return try {
            db.collection(collection(isPrivate)).document(roomId)
                .update("pinnedBy.$userId", pinned).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun clearMessages(roomId: String, isPrivate: Boolean): Result<Unit> {
        return try {
            val messagesRef = db.collection(collection(isPrivate)).document(roomId).collection("messages")
            val batch = db.batch()
            messagesRef.get().await().forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reportBug(userId: String, description: String, roomId: String? = null): Result<Unit> {
        return try {
            db.collection("bug_reports").add(mapOf(
                "userId" to userId, "description" to description,
                "roomId" to (roomId ?: ""), "timestamp" to System.currentTimeMillis(),
                "appVersion" to "1.0", "resolved" to false
            )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun setTyping(roomId: String, isPrivate: Boolean, userId: String, isTyping: Boolean) {
        try {
            db.collection(collection(isPrivate)).document(roomId)
                .update("typingUsers.$userId", isTyping).await()
        } catch (e: Exception) { }
    }

    fun observeTyping(roomId: String, isPrivate: Boolean, currentUserId: String): Flow<List<String>> = callbackFlow {
        val listener = db.collection(collection(isPrivate)).document(roomId)
            .addSnapshotListener { snapshot, _ ->
                val typing = (snapshot?.get("typingUsers") as? Map<*, *>)
                    ?.filter { (k, v) -> k != currentUserId && v == true }
                    ?.keys?.mapNotNull { it as? String } ?: emptyList()
                trySend(typing)
            }
        awaitClose { listener.remove() }
    }
    // Apagar mensagem autodestrutiva expirada
    suspend fun deleteMessage(roomId: String, isPrivate: Boolean, messageId: String) {
        try {
            db.collection(collection(isPrivate)).document(roomId)
                .collection("messages").document(messageId)
                .delete().await()
        } catch (e: Exception) { }
    }

    // Enviar mensagem com autodestruição
    suspend fun sendSelfDestructMessage(
        roomId: String, isPrivate: Boolean, text: String,
        senderId: String, senderName: String,
        destructAfterSeconds: Int
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val message = FirestoreMessage(
                text = text,
                senderId = senderId,
                senderName = senderName,
                timestamp = now,
                messageType = "selfdestruct",
                deliveredTo = mapOf(senderId to true),
                selfDestructAt = now + (destructAfterSeconds * 1000L)
            )
            val roomRef = db.collection(collection(isPrivate)).document(roomId)
            roomRef.collection("messages").add(message).await()
            roomRef.update(mapOf(
                "lastMessage" to "💣 Mensagem autodestrutiva",
                "lastMessageAt" to now
            )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}