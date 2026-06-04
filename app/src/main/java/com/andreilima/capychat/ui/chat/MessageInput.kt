package com.andreilima.capychat.ui.chat

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    onImageSelected: (Uri) -> Unit = {},
    onVideoSelected: (Uri) -> Unit = {},
    onFileSelected: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hasText = messageText.isNotBlank()
    var showAttachSheet by remember { mutableStateOf(false) }

    if (showAttachSheet) {
        AttachmentBottomSheet(
            onDismiss = { showAttachSheet = false },
            onImageSelected = onImageSelected,
            onVideoSelected = onVideoSelected,
            onFileSelected = onFileSelected
        )
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        IconButton(onClick = { showAttachSheet = true; onAttachClick() }) {
            Icon(
                Icons.Outlined.AddCircleOutline, null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageChange,
            placeholder = { Text("Mensagem") },
            modifier = Modifier
                .weight(1f)
                .animateContentSize(),
            shape = RoundedCornerShape(28.dp),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
        Spacer(Modifier.width(8.dp))
        FloatingActionButton(
            onClick = {
                if (hasText) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSendClick()
                }
            },
            modifier = Modifier.size(52.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
            shape = CircleShape
        ) {
            AnimatedContent(
                targetState = hasText,
                transitionSpec = { (scaleIn() + fadeIn()).togetherWith(scaleOut() + fadeOut()) },
                label = "send_icon"
            ) { isSending ->
                Icon(
                    imageVector = if (isSending) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.Mic,
                    contentDescription = if (isSending) "Enviar" else "Gravar"
                )
            }
        }
    }
}