package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.ui.components.CapyTopBar

@Composable
fun StatusViewerScreen(
    statusName: String,
    statusEmoji: String,
    statusText: String,
    statusTime: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
    ) {
        CapyTopBar(title = "Status", onBackClick = onBackClick)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(statusEmoji, fontSize = 72.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusName, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(statusText, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(statusTime, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}