package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.data.model.StatusItem
import com.andreilima.capychat.ui.components.CapyTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    statuses: List<StatusItem>,
    onStatusClick: (StatusItem) -> Unit,
    onPostStatus: (String, String) -> Unit,
) {
    var showComposer by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            CapyTopBar(title = "Status")
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
            ) {
                item {
                    Text(
                        "Recentes",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (statuses.isEmpty()) {
                    item {
                        Text(
                            "Nenhum status no momento. Por que não cria um?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(statuses) { status ->
                        StatusRowItem(status = status) { onStatusClick(status) }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showComposer = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Outlined.Add, "Novo Status")
        }
    }

    if (showComposer) {
        ModalBottomSheet(onDismissRequest = { showComposer = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("O que você está pensando?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { if (it.length <= 60) statusText = it },
                    placeholder = { Text("Estudando muito...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    supportingText = { Text("${statusText.length}/60") }
                )
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (statusText.isNotBlank()) {
                            onPostStatus(statusText, "🦫")
                            showComposer = false
                            statusText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Postar Status")
                }
            }
        }
    }
}

@Composable
private fun StatusRowItem(status: StatusItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(
                        width = 2.dp,
                        color = if (status.isNew) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(status.emoji, fontSize = 28.sp)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(status.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    status.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
