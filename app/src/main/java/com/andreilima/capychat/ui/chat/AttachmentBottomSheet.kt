package com.andreilima.capychat.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AttachmentOption(
    val icon: ImageVector,
    val label: String,
    val containerColor: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    onVideoSelected: (Uri) -> Unit,
    onFileSelected: (Uri) -> Unit
) {
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onImageSelected(it); onDismiss() } }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onVideoSelected(it); onDismiss() } }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onFileSelected(it); onDismiss() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Enviar",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            val options = listOf(
                AttachmentOption(
                    icon = Icons.Outlined.Image,
                    label = "Imagem",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { imageLauncher.launch("image/*") }
                ),
                AttachmentOption(
                    icon = Icons.Outlined.Videocam,
                    label = "Vídeo",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { videoLauncher.launch("video/*") }
                ),
                AttachmentOption(
                    icon = Icons.Outlined.AttachFile,
                    label = "Arquivo",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = { fileLauncher.launch("*/*") }
                ),
                AttachmentOption(
                    icon = Icons.Outlined.AudioFile,
                    label = "Áudio",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    onClick = { fileLauncher.launch("audio/*") }
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                options.forEach { option ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { option.onClick() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(option.containerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(option.label, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}