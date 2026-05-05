package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andreilima.capychat.data.model.StatusItem
import com.andreilima.capychat.ui.components.CapyTopBar
import com.andreilima.capychat.ui.components.StatusCard

@Composable
fun StatusScreen(
    statuses: List<StatusItem>,
    onStatusClick: (StatusItem) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        CapyTopBar(title = "Status", onBackClick = onBackClick)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(statuses) { status ->
                StatusCard(status = status) { onStatusClick(status) }
            }
        }
    }
}