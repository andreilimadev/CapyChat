package com.andreilima.capychat

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.andreilima.capychat.data.model.ChatItem
import com.andreilima.capychat.data.model.Message
import com.andreilima.capychat.data.model.StatusItem
import com.andreilima.capychat.ui.screens.*
import com.andreilima.capychat.ui.theme.CapyChatTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CapyChatApp() {
    CapyChatTheme {
        var currentScreen by remember { mutableStateOf("login") }
        var currentUserName by remember { mutableStateOf("") }

        // Listas imutáveis para recomposição correta (Regra 9)
        var chats by remember {
            mutableStateOf(
                listOf(
                    ChatItem("1", "Matemática", "Lista 3 vence amanhã", "Prof. Lucas", "📐", 2),
                    ChatItem("2", "História", "Alguém tem resumo da prova?", "Ana", "📚", 0),
                    ChatItem("3", "Projeto TCC", "Vamos revisar o app hoje", "Equipe", "🧠", 4)
                )
            )
        }

        var statuses by remember {
            mutableStateOf(
                listOf(
                    StatusItem("1", "Ana", "Estudando para prova", "📘", "há 10 min"),
                    StatusItem("2", "Lucas", "Terminando o slide", "💻", "há 25 min"),
                    StatusItem("3", "Marina", "Intervalo do café", "☕", "há 1 h")
                )
            )
        }

        var messagesByChat by remember {
            mutableStateOf(
                mapOf(
                    "1" to listOf(
                        Message("1", "Prof. Lucas", "Pessoal, revisem equações do 2º grau.", false, "08:30"),
                        Message("2", "Você", "Vou mandar minhas anotações depois.", true, "08:32")
                    ),
                    "2" to listOf(
                        Message("3", "Ana", "Alguém tem resumo da prova?", false, "09:10")
                    ),
                    "3" to listOf(
                        Message("4", "Equipe", "Vamos revisar o app hoje", false, "10:00")
                    )
                )
            )
        }

        var selectedChatId by remember { mutableStateOf<String?>(null) }
        var selectedStatusId by remember { mutableStateOf<String?>(null) }

        when (currentScreen) {
            "login" -> LoginScreen(
                onLoginClick = { email, _ ->
                    if (email.isNotBlank()) {
                        // Simulação de login robusta
                        currentUserName = email.substringBefore("@")
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        currentScreen = "home"
                    }
                },
                onGoToRegister = { currentScreen = "register" }
            )

            "register" -> RegisterScreen(
                onRegisterClick = { name, email, _ ->
                    if (name.isNotBlank() && email.isNotBlank()) {
                        currentUserName = name
                        currentScreen = "home"
                    }
                },
                onBackToLogin = { currentScreen = "login" }
            )

            "home" -> HomeScreen(
                userName = if (currentUserName.isBlank()) "Estudante" else currentUserName,
                onOpenConversations = { currentScreen = "conversations" },
                onOpenStatus = { currentScreen = "status" },
                onLogout = { 
                    currentUserName = ""
                    currentScreen = "login" 
                }
            )

            "conversations" -> ConversationsScreen(
                chats = chats,
                onChatClick = { chat ->
                    selectedChatId = chat.id
                    currentScreen = "chat"
                },
                onBackClick = { currentScreen = "home" },
                onCreateRoom = {
                    val newId = System.currentTimeMillis().toString()
                    val newRoom = ChatItem(
                        id = newId,
                        name = "Nova Sala #${chats.size + 1}",
                        lastMessage = "Sala iniciada",
                        author = currentUserName.ifBlank { "Você" },
                        avatarEmoji = "📝",
                        unreadCount = 0
                    )
                    chats = listOf(newRoom) + chats
                    messagesByChat = messagesByChat + (newId to listOf(
                        Message(newId + "_init", "Sistema", "Bem-vindo!", false, "Agora")
                    ))
                }
            )

            "chat" -> {
                val chatId = selectedChatId
                val chat = chats.find { it.id == chatId }
                if (chat != null) {
                    ChatScreen(
                        contactName = chat.name,
                        contactEmoji = chat.avatarEmoji,
                        messages = messagesByChat[chatId] ?: emptyList(),
                        onBackClick = { currentScreen = "conversations" },
                        onSendMessage = { text ->
                            if (text.isNotBlank()) {
                                val newMessage = Message(
                                    id = System.currentTimeMillis().toString(),
                                    sender = currentUserName.ifBlank { "Você" },
                                    text = text,
                                    isMine = true,
                                    time = "Agora"
                                )
                                messagesByChat = messagesByChat + (chatId!! to ((messagesByChat[chatId] ?: emptyList()) + newMessage))
                                
                                // Atualizar última mensagem na lista
                                chats = chats.map { 
                                    if (it.id == chatId) it.copy(lastMessage = text, author = "Você") else it 
                                }
                            }
                        }
                    )
                } else {
                    currentScreen = "conversations"
                }
            }

            "status" -> StatusScreen(
                statuses = statuses,
                onStatusClick = { status ->
                    selectedStatusId = status.id
                    currentScreen = "status_view"
                },
                onBackClick = { currentScreen = "home" }
            )

            "status_view" -> {
                val statusId = selectedStatusId
                val status = statuses.find { it.id == statusId }
                if (status != null) {
                    StatusViewerScreen(
                        statusName = status.name,
                        statusEmoji = status.emoji,
                        statusText = status.text,
                        statusTime = status.time,
                        onBackClick = { currentScreen = "status" }
                    )
                } else {
                    currentScreen = "status"
                }
            }
        }
    }
}
