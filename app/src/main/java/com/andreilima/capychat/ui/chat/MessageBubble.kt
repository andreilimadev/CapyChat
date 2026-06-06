package com.andreilima.capychat.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.andreilima.capychat.data.model.Message
import com.andreilima.capychat.data.model.MessageStatus

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    showName: Boolean,
    isGrouped: Boolean,
    isHighlighted: Boolean = false,
    currentUserId: String = "",
    onReply: (Message) -> Unit = {},
    onReact: (Message, String) -> Unit = { _, _ -> },
    onImageClick: (String) -> Unit = {}
) {
    val bubbleShape = if (message.isMine) {
        RoundedCornerShape(topStart = 20.dp, topEnd = if (isGrouped) 4.dp else 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp)
    } else {
        RoundedCornerShape(topStart = if (isGrouped) 4.dp else 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
    }

    val reactionEmojis = listOf("❤️", "😂", "👍", "😮", "😢", "🔥")
    var showActionMenu by remember { mutableStateOf(false) }
    val myCurrentEmoji = message.reactions[currentUserId]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isGrouped) 2.dp else 8.dp)
            .animateContentSize(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (showName) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                )
            }

            Box {
                Surface(
                    color = when {
                        isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                        message.isMine -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = when {
                        isHighlighted -> MaterialTheme.colorScheme.onTertiaryContainer
                        message.isMine -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = bubbleShape,
                    tonalElevation = if (message.isMine) 2.dp else 0.dp,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showActionMenu = true }
                    )
                ) {
                    Column(modifier = Modifier.padding(
                        // Imagem ocupa o bubble sem padding lateral
                        horizontal = if (message.messageType == "image") 0.dp else 14.dp,
                        vertical = if (message.messageType == "image") 0.dp else 8.dp
                    )) {

                        // Reply preview
                        if (message.replyToText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (message.isMine)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = if (message.messageType == "image") 8.dp else 0.dp,
                                        end = if (message.messageType == "image") 8.dp else 0.dp,
                                        top = if (message.messageType == "image") 8.dp else 0.dp,
                                        bottom = 6.dp
                                    )
                            ) {
                                Row(modifier = Modifier.padding(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp).height(32.dp)
                                            .background(
                                                if (message.isMine)
                                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                                else MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            message.replyToSender,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                            else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            message.replyToText,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // =========================================================
                        // CONTEÚDO PRINCIPAL — por tipo
                        // =========================================================
                        when (message.messageType) {

                            "image" -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(message.text)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Imagem",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 260.dp)
                                        .clip(bubbleShape)
                                        .clickable { onImageClick(message.text) }
                                )
                                // Hora sobre a imagem
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(end = 8.dp, bottom = 6.dp, top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = message.time,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    if (message.isMine) {
                                        val tickColor = when (message.status) {
                                            MessageStatus.READ -> Color(0xFF4FC3F7)
                                            else -> Color.White.copy(alpha = 0.8f)
                                        }
                                        Text(
                                            text = if (message.status == MessageStatus.SENT) "✓" else "✓✓",
                                            fontSize = 10.sp, color = tickColor, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            "video" -> {
                                Row(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { onImageClick(message.text) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayCircle,
                                        contentDescription = "Vídeo",
                                        modifier = Modifier.size(36.dp),
                                        tint = if (message.isMine)
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text("Vídeo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Toque para abrir", fontSize = 11.sp,
                                            color = if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                MessageTimeRow(message = message)
                            }

                            "audio" -> {
                                Row(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { onImageClick(message.text) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Audiotrack,
                                        contentDescription = "Áudio",
                                        modifier = Modifier.size(36.dp),
                                        tint = if (message.isMine)
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text("Áudio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Toque para ouvir", fontSize = 11.sp,
                                            color = if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                MessageTimeRow(message = message)
                            }

                            "file" -> {
                                Row(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { onImageClick(message.text) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        contentDescription = "Arquivo",
                                        modifier = Modifier.size(36.dp),
                                        tint = if (message.isMine)
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        val fileName = message.text.substringAfterLast("/")
                                            .substringBefore("?").take(30)
                                        Text(
                                            fileName.ifBlank { "Arquivo" },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("Toque para abrir", fontSize = 11.sp,
                                            color = if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                MessageTimeRow(message = message)
                            }
                            "selfdestruct" -> {
                                val remaining = remember(message.selfDestructAt) {
                                    ((message.selfDestructAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                                }
                                Row(
                                    modifier = Modifier.padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Timer,
                                        contentDescription = "Autodestrutiva",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (message.isMine)
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        else Color(0xFFFF6B35)
                                    )
                                    Column {
                                        Text(text = message.text, fontSize = 15.sp, lineHeight = 20.sp)
                                        Text(
                                            text = "💣 expira em ${remaining}s",
                                            fontSize = 10.sp,
                                            color = if (message.isMine)
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                            else Color(0xFFFF6B35).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                MessageTimeRow(message = message)
                            }

                            // "text" e qualquer outro tipo
                            else -> {
                                Text(text = message.text, fontSize = 15.sp, lineHeight = 20.sp)
                                MessageTimeRow(message = message)
                            }
                        }
                    }
                }

                // Menu de ações (long press)
                DropdownMenu(
                    expanded = showActionMenu,
                    onDismissRequest = { showActionMenu = false }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reactionEmojis.forEach { emoji ->
                            val isSelected = emoji == myCurrentEmoji
                            Box(
                                modifier = Modifier
                                    .size(40.dp).clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { onReact(message, emoji); showActionMenu = false },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, fontSize = 22.sp) }
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Responder") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Reply, null) },
                        onClick = { onReply(message); showActionMenu = false }
                    )
                }
            }

            // Reactions externas
            if (message.reactions.isNotEmpty()) {
                val grouped = message.reactions.values.groupBy { it }.mapValues { it.value.size }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                ) {
                    grouped.forEach { (emoji, count) ->
                        val isMine = myCurrentEmoji == emoji
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMine) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = if (isMine) 4.dp else 2.dp,
                            modifier = Modifier.clickable { onReact(message, emoji) }
                        ) {
                            Text(
                                text = if (count > 1) "$emoji $count" else emoji,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 12.sp,
                                color = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Componente auxiliar para hora + ticks — evita repetição
@Composable
private fun ColumnScope.MessageTimeRow(message: Message) {
    Row(
        modifier = Modifier
            .align(Alignment.End)
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = message.time,
            fontSize = 10.sp,
            color = (if (message.isMine) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)
        )
        if (message.isMine) {
            val tickColor = when (message.status) {
                MessageStatus.READ -> Color(0xFF4FC3F7)
                MessageStatus.DELIVERED -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                MessageStatus.SENT -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
            }
            Text(
                text = if (message.status == MessageStatus.SENT) "✓" else "✓✓",
                fontSize = 10.sp, color = tickColor, fontWeight = FontWeight.Bold
            )
        }
    }
}