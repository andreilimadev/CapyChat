package com.andreilima.capychat.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.data.model.StatusItem
import com.andreilima.capychat.ui.components.CapyEmptyState
import com.andreilima.capychat.ui.components.CapyTopBar

private val STATUS_EMOJIS = listOf(
    "🦫", "😊", "😎", "🤔", "😴", "🔥", "💪", "🎉",
    "❤️", "🎮", "🎵", "📚", "✈️", "🍕", "☕", "🌙"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    statuses: List<StatusItem>,
    onStatusClick: (StatusItem) -> Unit,
    onPostStatus: (String, String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var showComposer by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🦫") }

    // Separar meus status dos outros (por ora todos juntos, separação visual futura)
    val newStatuses = statuses.filter { it.isNew }
    val seenStatuses = statuses.filter { !it.isNew }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CapyTopBar(title = "Status")

            if (statuses.isEmpty()) {
                CapyEmptyState(
                    emoji = "📖",
                    title = "Nenhum status ainda",
                    description = "Seus contatos ainda não postaram nada. Que tal começar?"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
                ) {
                    if (newStatuses.isNotEmpty()) {
                        item {
                            Text(
                                "Novos",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(newStatuses, key = { it.id }) { status ->
                            StatusRowItem(status = status) { onStatusClick(status) }
                        }
                    }

                    if (seenStatuses.isNotEmpty()) {
                        item {
                            Text(
                                "Vistos",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = if (newStatuses.isNotEmpty()) 8.dp else 0.dp)
                            )
                        }
                        items(seenStatuses, key = { it.id }) { status ->
                            StatusRowItem(status = status, dimmed = true) { onStatusClick(status) }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showComposer = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Outlined.Add, "Novo Status")
        }
    }

    // Composer
    if (showComposer) {
        ModalBottomSheet(
            onDismissRequest = {
                showComposer = false
                statusText = ""
                selectedEmoji = "🦫"
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Novo Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Preview do status
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(selectedEmoji, fontSize = 26.sp)
                        }
                        Column {
                            Text(
                                text = if (statusText.isBlank()) "Seu status vai aparecer aqui..." else statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (statusText.isBlank())
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "agora",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Emoji picker horizontal
                Text(
                    "Escolha um emoji",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(STATUS_EMOJIS) { emoji ->
                        val isSelected = emoji == selectedEmoji
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            animationSpec = tween(200),
                            label = "emoji_bg"
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ) else Modifier
                                )
                                .clickable {
                                    selectedEmoji = emoji
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = statusText,
                    onValueChange = { if (it.length <= 80) statusText = it },
                    placeholder = { Text("O que está acontecendo?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Expira em 24 horas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${statusText.length}/80")
                        }
                    }
                )

                Button(
                    onClick = {
                        if (statusText.isNotBlank()) {
                            onPostStatus(statusText, selectedEmoji)
                            showComposer = false
                            statusText = ""
                            selectedEmoji = "🦫"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = statusText.isNotBlank()
                ) {
                    Icon(Icons.Outlined.AutoStories, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Publicar")
                }
            }
        }
    }
}

@Composable
private fun StatusRowItem(
    status: StatusItem,
    dimmed: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = if (status.isNew)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .border(width = if (status.isNew) 2.5.dp else 1.5.dp, color = borderColor, shape = CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = if (dimmed) 0.5f else 1f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    status.emoji,
                    fontSize = 28.sp,
                    color = LocalContentColor.current.copy(alpha = if (dimmed) 0.6f else 1f)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    status.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dimmed) 0.6f else 1f)
                )
                Text(
                    status.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dimmed) 0.5f else 1f)
                )
            }

            // Indicador de não lido
            if (status.isNew) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}