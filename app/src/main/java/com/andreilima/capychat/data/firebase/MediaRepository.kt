package com.andreilima.capychat.data.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object MediaRepository {

    private val storage: FirebaseStorage get() = FirebaseStorage.getInstance()
    private val storageRef: StorageReference get() = storage.reference

    // =========================================================
    // UPLOAD GENÉRICO COM PROGRESSO
    // =========================================================

    sealed class UploadState {
        data class Progress(val percent: Int) : UploadState()
        data class Success(val downloadUrl: String) : UploadState()
        data class Error(val message: String) : UploadState()
    }

    fun uploadFile(
        uri: Uri,
        path: String  // ex: "chat_media/roomId/filename.jpg"
    ): Flow<UploadState> = callbackFlow {
        val ref = storageRef.child(path)
        val uploadTask = ref.putFile(uri)

        uploadTask.addOnProgressListener { snapshot ->
            val percent = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
            trySend(UploadState.Progress(percent))
        }

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception!!
            ref.downloadUrl
        }.addOnSuccessListener { uri ->
            trySend(UploadState.Success(uri.toString()))
            close()
        }.addOnFailureListener { e ->
            trySend(UploadState.Error(e.message ?: "Erro no upload"))
            close()
        }

        awaitClose { uploadTask.cancel() }
    }

    // =========================================================
    // HELPERS DE PATH POR TIPO
    // =========================================================

    fun imagePathForRoom(roomId: String, fileName: String) =
        "chat_media/$roomId/images/$fileName"

    fun videoPathForRoom(roomId: String, fileName: String) =
        "chat_media/$roomId/videos/$fileName"

    fun audioPathForRoom(roomId: String, fileName: String) =
        "chat_media/$roomId/audio/$fileName"

    fun filePathForRoom(roomId: String, fileName: String) =
        "chat_media/$roomId/files/$fileName"

    fun avatarPath(userId: String) =
        "avatars/$userId/profile.jpg"

    // =========================================================
    // DELETAR MÍDIA
    // =========================================================

    suspend fun deleteFile(downloadUrl: String): Result<Unit> {
        return try {
            storage.getReferenceFromUrl(downloadUrl).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}