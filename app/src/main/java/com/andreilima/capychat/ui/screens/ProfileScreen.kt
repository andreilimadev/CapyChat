package com.andreilima.capychat.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andreilima.capychat.ui.components.CapyTopBar
import com.andreilima.capychat.ui.utils.pressEffect
import com.andreilima.capychat.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay

private val AVATAR_EMOJIS = listOf(
    "🐾", "🦫", "🐱", "🐶", "🐻", "🦊", "🐼", "🐨",
    "🦁", "🐯", "🐸", "🐧", "🦆", "🐺", "🦝", "🐮",
    "🐷", "🐭", "🐹", "🐰", "🦋", "🐙", "🦈", "🐬"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenCIA: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current
    val userProfile by chatViewModel.currentUserProfile.collectAsStateWithLifecycle()
    val isLoading by chatViewModel.isProfileSaving.collectAsStateWithLifecycle()

    var showEditSheet by remember { mutableStateOf(false) }
    var logoTaps by remember { mutableStateOf(0) }

    var editedName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: userName) }
    var editedBio by remember(userProfile) { mutableStateOf(userProfile?.bio ?: "") }
    var editedEmoji by remember(userProfile) { mutableStateOf(userProfile?.avatarEmoji ?: "🐾") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveSuccess by remember { mutableStateOf(false) }

    // Animação de entrada da tela
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(logoTaps) {
        if (logoTaps > 0) { delay(2000); logoTaps = 0 }
    }
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) { delay(2000); saveSuccess = false }
    }

    // Bottom Sheet de edição
    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Editar Perfil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Avatar com animação de escala ao trocar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val emojiScale by animateFloatAsState(
                        targetValue = if (showEmojiPicker) 0.9f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "emoji_scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .scale(emojiScale)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEmojiPicker = !showEmojiPicker
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(editedEmoji, fontSize = 32.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Toque para trocar o avatar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Grid de emojis com AnimatedVisibility
                AnimatedVisibility(
                    visible = showEmojiPicker,
                    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(8),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(AVATAR_EMOJIS) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (emoji == editedEmoji)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else
                                                MaterialTheme.colorScheme.surface
                                        )
                                        .clickable {
                                            editedEmoji = emoji
                                            showEmojiPicker = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Nome de exibição") },
                    leadingIcon = { Icon(Icons.Outlined.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = editedBio,
                    onValueChange = { if (it.length <= 120) editedBio = it },
                    label = { Text("Bio") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    placeholder = { Text("Conte algo sobre você...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    supportingText = { Text("${editedBio.length}/120") }
                )

                AnimatedVisibility(visible = saveError != null) {
                    saveError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = {
                        val uid = userProfile?.uid ?: return@Button
                        chatViewModel.updateProfile(
                            uid = uid,
                            displayName = editedName.trim(),
                            bio = editedBio.trim(),
                            avatarEmoji = editedEmoji,
                            onSuccess = {
                                saveSuccess = true
                                showEditSheet = false
                                saveError = null
                            },
                            onError = { saveError = it }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = editedName.isNotBlank() && !isLoading
                ) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "save_btn"
                    ) { loading ->
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }

    // Tela principal com animação de entrada
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { it / 12 },
            animationSpec = tween(300)
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CapyTopBar(title = "Perfil")

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar com animação de bounce ao atualizar
                            val avatarScale by animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "avatar_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(avatarScale)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        logoTaps++
                                        if (logoTaps >= 7) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onOpenCIA()
                                            logoTaps = 0
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile?.avatarEmoji ?: "🐾",
                                    fontSize = 48.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = userProfile?.displayName ?: userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Text(
                                text = userEmail,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            userProfile?.userTag?.let { tag ->
                                if (tag.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Text(
                                            text = "@$tag",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            val bio = userProfile?.bio ?: ""
                            AnimatedVisibility(visible = bio.isNotEmpty()) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = bio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Banner de sucesso
                            AnimatedVisibility(
                                visible = saveSuccess,
                                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.CheckCircle, null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Perfil atualizado!",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = { showEditSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Outlined.Edit, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Editar perfil")
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            ProfileOptionRow(icon = Icons.Outlined.Lock, label = "Privacidade", onClick = onOpenPrivacy)
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ProfileOptionRow(icon = Icons.Outlined.Settings, label = "Configurações", onClick = onOpenSettings)
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ProfileOptionRow(icon = Icons.Outlined.HelpOutline, label = "Ajuda", onClick = onOpenHelp)
                        }
                    }
                }

                item {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Sair da conta")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressEffect(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(label, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    }
}