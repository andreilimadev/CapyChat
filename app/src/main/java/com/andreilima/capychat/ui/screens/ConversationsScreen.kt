package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andreilima.capychat.data.model.ChatItem
import com.andreilima.capychat.ui.components.CapyTopBar
import com.andreilima.capychat.ui.components.ChatCard
import com.andreilima.capychat.ui.components.PrimaryButton

@Composable
fun ConversationsScreen(
    chats: List<ChatItem>,
    onChatClick: (ChatItem) -> Unit,
    onBackClick: () -> Unit,
    onCreateRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        CapyTopBar(title = "Conversas", onBackClick = onBackClick)
        PrimaryButton(text = "Criar nova sala", onClick = onCreateRoom)
        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(chats) { chat ->
                ChatCard(chat = chat) { onChatClick(chat) }
            }
        }
    }
}