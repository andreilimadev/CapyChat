package com.andreilima.capychat.data.firebase

import com.andreilima.capychat.data.model.FirestoreUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object UserRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun getUser(uid: String): FirestoreUser? {
        return try {
            db.collection("users").document(uid).get().await()
                .toObject(FirestoreUser::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun updateUserProfile(
        uid: String, displayName: String, bio: String,
        avatarEmoji: String = "🐾", photoUrl: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName,
                "bio" to bio,
                "avatarEmoji" to avatarEmoji
            )
            if (photoUrl != null) updates["photoUrl"] = photoUrl
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun setOnlineStatus(uid: String, isOnline: Boolean) {
        try {
            db.collection("users").document(uid).update(
                mapOf("isOnline" to isOnline, "lastSeen" to System.currentTimeMillis())
            ).await()
        } catch (e: Exception) { }
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
        val normalized = query.lowercase().trim()
        return try {
            db.collection("users")
                .orderBy("searchableUsername")
                .startAt(normalized)
                .endAt(normalized + "\uf8ff")
                .limit(20)
                .get().await()
                .toObjects(FirestoreUser::class.java)
        } catch (e: Exception) { emptyList() }
    }
    suspend fun setGhostMode(uid: String, enabled: Boolean): Result<Unit> {
        return try {
            db.collection("users").document(uid).update(
                mapOf("isGhostMode" to enabled, "isOnline" to !enabled)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}