package com.andreilima.capychat.data.firebase

import com.andreilima.capychat.data.model.FirestoreUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

object AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser
    val currentUserId get() = auth.currentUser?.uid

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<Unit> {
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

    suspend fun logout() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                db.collection("users").document(uid).update(
                    mapOf("isOnline" to false, "lastSeen" to System.currentTimeMillis())
                ).await()
            } catch (e: Exception) { }
        }
        auth.signOut()
    }
}