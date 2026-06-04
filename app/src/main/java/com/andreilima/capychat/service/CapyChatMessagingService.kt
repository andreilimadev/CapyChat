package com.andreilima.capychat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.andreilima.capychat.MainActivity
import com.andreilima.capychat.R
import com.andreilima.capychat.data.firebase.NotificationRepository
import com.andreilima.capychat.data.model.FirestoreNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CapyChatMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val CHANNEL_ID = "capychat_messages"
        const val CHANNEL_NAME = "Mensagens"
        private var notificationId = 0
    }

    // =========================================================
    // TOKEN NOVO — salvar no Firestore
    // =========================================================

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
    }

    // =========================================================
    // MENSAGEM RECEBIDA
    // =========================================================

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val title = message.notification?.title
            ?: data["senderName"]
            ?: "CapyChat"
        val body = message.notification?.body
            ?: data["text"]
            ?: "Nova mensagem"

        val fromUserId = data["fromUserId"] ?: ""
        val toUserId = data["toUserId"] ?: ""
        val type = data["type"] ?: "message"
        val payload = data["payload"] ?: ""

        // Salva no Firestore como notificação persistente
        if (toUserId.isNotBlank() && fromUserId.isNotBlank()) {
            serviceScope.launch {
                NotificationRepository.createNotification(
                    FirestoreNotification(
                        toUserId = toUserId,
                        fromUserId = fromUserId,
                        fromUsername = title,
                        type = type,
                        payload = payload,
                        isRead = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }

        showNotification(title, body)
    }

    // =========================================================
    // EXIBIR NOTIFICAÇÃO LOCAL
    // =========================================================

    private fun showNotification(title: String, body: String) {
        createChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId++, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de mensagens do CapyChat"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}