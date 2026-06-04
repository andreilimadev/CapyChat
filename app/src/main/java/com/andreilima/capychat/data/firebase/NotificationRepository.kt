package com.andreilima.capychat.data.firebase

import com.andreilima.capychat.data.model.FirestoreNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object NotificationRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun createNotification(notification: FirestoreNotification): Result<Unit> {
        return try {
            db.collection("notifications").add(notification).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun observeAllNotifications(userId: String): Flow<List<FirestoreNotification>> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("toUserId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
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
                .update("isRead", true).await()
        } catch (e: Exception) { }
    }
}