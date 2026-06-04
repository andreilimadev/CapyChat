package com.andreilima.capychat.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andreilima.capychat.data.model.FirestoreNotification
import com.andreilima.capychat.ui.components.CapyEmptyState
import com.andreilima.capychat.ui.components.CapyTopBar
import com.andreilima.capychat.ui.viewmodel.ChatViewModel

@Composable
fun NotificationsScreen(
    onNotificationClick: (FirestoreNotification) -> Unit = {},
    chatViewModel: ChatViewModel = viewModel()
) {
    val notifications by chatViewModel.notifications.collectAsStateWithLifecycle()
    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()

    // Zera badge ao entrar na tela
    LaunchedEffect(Unit) {
        chatViewModel.markAllNotificationsRead()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CapyTopBar(
            title = "Notificações",
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            CapyEmptyState(
                emoji = "🔔",
                title = "Sem notificações",
                description = "Você está em dia! Nenhuma notificação nova."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationCard(
                        notification = notif,
                        onClick = {
                            chatViewModel.markNotificationRead(notif.id)
                            onNotificationClick(notif)
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: FirestoreNotification,
    onClick: () -> Unit
) {
    val (icon, iconTint, bgColor) = notifStyle(notification.type)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (!notification.isRead)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = if (!notification.isRead) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Conteúdo
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.fromUsername.ifBlank { "CapyChat" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Text(
                    text = notifBody(notification),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Text(
                    text = formatTimestamp(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun notifStyle(type: String): Triple<ImageVector, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    return when (type) {
        "message" -> Triple(
            Icons.Outlined.ChatBubbleOutline,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
        "mention" -> Triple(
            Icons.Outlined.AlternateEmail,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        "status" -> Triple(
            Icons.Outlined.AutoStories,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer
        )
        else -> Triple(
            Icons.Outlined.Notifications,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun notifBody(n: FirestoreNotification): String {
    return when (n.type) {
        "message" -> if (n.payload.isNotBlank()) "\"${n.payload}\"" else "Enviou uma mensagem"
        "mention" -> "Mencionou você: \"${n.payload}\""
        "status" -> "Publicou um novo status"
        else -> n.payload.ifBlank { "Nova notificação" }
    }
}

private fun formatTimestamp(ts: Long): String {
    if (ts <= 0) return ""
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "agora"
        diff < 3_600_000 -> "há ${diff / 60_000}m"
        diff < 86_400_000 -> "há ${diff / 3_600_000}h"
        else -> "há ${diff / 86_400_000}d"
    }
}