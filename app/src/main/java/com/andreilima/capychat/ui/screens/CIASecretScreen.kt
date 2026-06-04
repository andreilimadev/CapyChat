package com.andreilima.capychat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andreilima.capychat.data.firebase.FirebaseService
import com.andreilima.capychat.ui.components.CapyTopBar
import com.andreilima.capychat.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CIASecretScreen(
    onBackClick: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    var showGlitch by remember { mutableStateOf(false) }
    var capyRainActive by remember { mutableStateOf(false) }
    var retroModeActive by remember { mutableStateOf(false) }
    var ghostModeActive by remember { mutableStateOf(false) }
    var showSelfDestructDialog by remember { mutableStateOf(false) }
    var selfDestructText by remember { mutableStateOf("") }
    var selfDestructTimer by remember { mutableStateOf(10) }
    val scope = rememberCoroutineScope()


    val currentUser by chatViewModel.currentUserProfile.collectAsStateWithLifecycle()
    val uid = FirebaseService.auth.currentUser?.uid ?: ""

    // Sincronizar ghost mode com o perfil real
    LaunchedEffect(currentUser) {
        currentUser?.let { ghostModeActive = it.isGhostMode }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (retroModeActive) Color(0xFF001A00) else Color(0xFF0A0A0A))
    ) {
        if (capyRainActive) CapyRainOverlay()
        if (retroModeActive) ScanlineOverlay()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = { CapyTopBar(title = "CIA LABS", onBackClick = onBackClick) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TerminalHeader()

                // ── GHOST MODE (funcional) ──────────────────────────
                CIAToggleCard(
                    title = "Ghost Mode",
                    description = if (ghostModeActive)
                        "ATIVO — você aparece offline para todos"
                    else
                        "Apareça como offline enquanto usa o app",
                    icon = Icons.Outlined.VisibilityOff,
                    isActive = ghostModeActive,
                    onToggle = {
                        chatViewModel.toggleGhostMode(uid, !ghostModeActive) { enabled ->
                            ghostModeActive = enabled
                        }
                    }
                )

                // ── SELF DESTRUCT (funcional) ───────────────────────
                CIAFeatureCard(
                    title = "Mensagem Autodestrutiva",
                    description = "Mensagem apagada automaticamente após o tempo definido",
                    icon = Icons.Outlined.Timer,
                    isActive = false
                ) { showSelfDestructDialog = true }

                // ── VISUAL: CAPYVIRUS ───────────────────────────────
                CIAFeatureCard(
                    title = "CapyVirus Simulation",
                    description = "Simulação visual de corrupção de sistema. 100% seguro.",
                    icon = Icons.Outlined.Warning,
                    isActive = showGlitch
                ) {
                    scope.launch {
                        showGlitch = true
                        delay(3000)
                        showGlitch = false
                    }
                }

                // ── VISUAL: CAPY RAIN ───────────────────────────────
                CIAFeatureCard(
                    title = "Capy Rain",
                    description = "Ativa precipitação atmosférica de capivaras digitais.",
                    icon = Icons.Outlined.CloudDownload,
                    isActive = capyRainActive
                ) { capyRainActive = !capyRainActive }

                // ── VISUAL: RETRO MODE ──────────────────────────────
                CIAFeatureCard(
                    title = "Retro Mode",
                    description = "Habilita interface analógica de baixa resolução.",
                    icon = Icons.Outlined.SettingsBackupRestore,
                    isActive = retroModeActive
                ) { retroModeActive = !retroModeActive }

                Spacer(Modifier.height(32.dp))
                Text(
                    "CONNECTED TO MILHO_DATABASE_V4",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Green.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (showGlitch) GlitchOverlay()
    }

    // ── SELF DESTRUCT DIALOG ────────────────────────────────────
    if (showSelfDestructDialog) {
        AlertDialog(
            onDismissRequest = { showSelfDestructDialog = false },
            containerColor = Color(0xFF0A0A0A),
            icon = {
                Icon(Icons.Outlined.Timer, null, tint = Color.Green)
            },
            title = {
                Text(
                    "MENSAGEM AUTODESTRUTIVA",
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = selfDestructText,
                        onValueChange = { selfDestructText = it },
                        placeholder = {
                            Text("Mensagem secreta...", color = Color.Gray)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Green,
                            unfocusedBorderColor = Color.Green.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Text(
                        "Autodestruição em: ${selfDestructTimer}s",
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = selfDestructTimer.toFloat(),
                        onValueChange = { selfDestructTimer = it.toInt() },
                        valueRange = 5f..300f,
                        steps = 58,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Green,
                            activeTrackColor = Color.Green,
                            inactiveTrackColor = Color.Green.copy(alpha = 0.2f)
                        )
                    )
                    Text(
                        when {
                            selfDestructTimer < 60 -> "${selfDestructTimer}s"
                            else -> "${selfDestructTimer / 60}m ${selfDestructTimer % 60}s"
                        },
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Precisará do roomId atual — por ora salva no chat ativo
                        // Será conectado na próxima iteração via ChatScreen
                        showSelfDestructDialog = false
                        selfDestructText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    enabled = selfDestructText.isNotBlank()
                ) {
                    Text("ENVIAR", color = Color.Black, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelfDestructDialog = false }) {
                    Text("ABORTAR", color = Color.Red, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

// ── COMPONENTES ──────────────────────────────────────────────────

@Composable
private fun TerminalHeader() {
    Column {
        Text(
            "CAPY INTELLIGENCE AGENCY",
            color = Color.Green,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            "Experimental Systems Division",
            color = Color.Green.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color.Green.copy(alpha = 0.3f))
    }
}

@Composable
private fun CIAFeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false,
    onActivate: () -> Unit
) {
    Surface(
        onClick = onActivate,
        color = if (isActive) Color.Green.copy(alpha = 0.1f) else Color.DarkGray.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) Color.Green.copy(alpha = 0.6f) else Color.Green.copy(alpha = 0.2f)
        )
    ) {
        ListItem(
            headlineContent = {
                Text(title, color = Color.White, fontFamily = FontFamily.Monospace)
            },
            supportingContent = {
                Text(description, color = Color.LightGray, fontSize = 12.sp)
            },
            leadingContent = {
                Icon(icon, null, tint = if (isActive) Color.Green else Color.Green.copy(alpha = 0.5f))
            },
            trailingContent = {
                if (isActive) {
                    Text("ON", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun CIAToggleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        color = if (isActive) Color.Green.copy(alpha = 0.1f) else Color.DarkGray.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) Color.Green.copy(alpha = 0.6f) else Color.Green.copy(alpha = 0.2f)
        )
    ) {
        ListItem(
            headlineContent = {
                Text(title, color = Color.White, fontFamily = FontFamily.Monospace)
            },
            supportingContent = {
                Text(description, color = if (isActive) Color.Green.copy(alpha = 0.7f) else Color.LightGray, fontSize = 12.sp)
            },
            leadingContent = {
                Icon(icon, null, tint = if (isActive) Color.Green else Color.Green.copy(alpha = 0.5f))
            },
            trailingContent = {
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.Green,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun GlitchOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "glitch")
    val offset by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.1f))
            .graphicsLayer(translationX = offset)
    ) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text("SYSTEM_ERROR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            Text("INJECTING_MILHO.DLL", color = Color.Green, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun CapyRainOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = -100f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "y"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val count = 15
        for (i in 0 until count) {
            val x = (size.width / count) * i
            val currentY = (yOffset + (i * 200)) % size.height
            drawCircle(Color.Green.copy(alpha = 0.3f), radius = 4f, center = Offset(x, currentY))
        }
    }
}

@Composable
private fun ScanlineOverlay() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
        val lineCount = 100
        for (i in 0 until lineCount) {
            val y = (size.height / lineCount) * i
            drawLine(Color.Green, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        }
    }
}