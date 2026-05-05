package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.data.model.Message
import com.andreilima.capychat.ui.components.CapyTopBar
import com.andreilima.capychat.ui.components.MessageBubble
import com.andreilima.capychat.ui.components.MessageInput

@Composable
fun ChatScreen(
    contactName: String,
    contactEmoji: String,
    messages: List<Message>,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        CapyTopBar(title = "$contactEmoji $contactName", onBackClick = onBackClick)
        Text("Online agora", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        MessageInput(
            value = input,
            onValueChange = { input = it },
            onSendClick = {
                if (input.isNotBlank()) {
                    onSendMessage(input)
                    input = ""
                }
            }
        )
    }
}