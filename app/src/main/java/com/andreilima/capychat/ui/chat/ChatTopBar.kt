package com.andreilima.capychat.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    contactName: String,
    contactEmoji: String,
    isContactOnline: Boolean,
    typingUsers: List<String>,
    showSearch: Boolean,
    searchQuery: String,
    filteredCount: Int,
    onBackClick: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onViewMedia: () -> Unit = {},
    onMute: () -> Unit,
    onPin: () -> Unit,
    onClearConversation: () -> Unit,
    onReportBug: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(contactEmoji, fontSize = 20.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            contactName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val statusText = when {
                            typingUsers.isNotEmpty() -> "digitando..."
                            isContactOnline -> "online agora"
                            else -> ""
                        }
                        AnimatedVisibility(visible = statusText.isNotEmpty()) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (typingUsers.isNotEmpty())
                                    MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar")
                }
            },
            actions = {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        if (showSearch) Icons.Outlined.SearchOff else Icons.Outlined.Search,
                        "Buscar"
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, "Mais")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Ver mídia") },
                            leadingIcon = { Icon(Icons.Outlined.PermMedia, null) },
                            onClick = { showMenu = false; onViewMedia() }
                        )
                        DropdownMenuItem(
                            text = { Text("Silenciar") },
                            leadingIcon = { Icon(Icons.Outlined.NotificationsOff, null) },
                            onClick = { showMenu = false; onMute() }
                        )
                        DropdownMenuItem(
                            text = { Text("Fixar conversa") },
                            leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                            onClick = { showMenu = false; onPin() }
                        )
                        DropdownMenuItem(
                            text = { Text("Limpar conversa") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) },
                            onClick = { showMenu = false; onClearConversation() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Reportar bug") },
                            leadingIcon = { Icon(Icons.Outlined.BugReport, null) },
                            onClick = { showMenu = false; onReportBug() }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // SearchBar inline
        AnimatedVisibility(
            visible = showSearch,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar na conversa...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Outlined.Clear, null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "$filteredCount resultado(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}