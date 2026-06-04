package com.andreilima.capychat.data.firebase

import com.andreilima.capychat.data.model.FirestoreStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object StatusRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    fun observeStatuses(): Flow<List<FirestoreStatus>> = callbackFlow {
        val listener = db.collection("statuses")
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirestoreStatus::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createStatus(
        userId: String, username: String,
        text: String, emoji: String, photoUrl: String? = null
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            db.collection("statuses").add(
                FirestoreStatus(
                    userId = userId, username = username, text = text, emoji = emoji,
                    photoUrl = photoUrl, createdAt = now, expiresAt = now + (24 * 60 * 60 * 1000)
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun markStatusAsViewed(statusId: String, userId: String) {
        try {
            db.collection("statuses").document(statusId)
                .update("viewedBy.$userId", true).await()
        } catch (e: Exception) { }
    }
}