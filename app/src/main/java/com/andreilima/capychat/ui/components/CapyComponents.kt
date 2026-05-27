package com.andreilima.capychat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.data.model.ChatItem
import com.andreilima.capychat.data.model.Message
import com.andreilima.capychat.data.model.StatusItem

val ROOM_EMOJIS = listOf("💬", "📐", "📚", "🧠", "📝", "🎨", "💻", "🧪", "🍕", "⚽")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapyTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                }
            }
        },
        actions = {
            if (onLogoutClick != null) {
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.AutoMirrored.Filled.Logout, "Sair")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun CreateRoomDialogContent(
    roomName: String,
    roomEmoji: String,
    onNameChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = roomName,
            onValueChange = onNameChange,
            label = { Text("Nome da Sala") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Text("Escolha um emoji:", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ROOM_EMOJIS) { emoji ->
                val isSelected = emoji == roomEmoji
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onEmojiChange(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 20.sp)
                }
            }
        }
    }
}
