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
import com.andreilima.capychat.ui.components.CapyTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CIASecretScreen(
    onBackClick: () -> Unit,
) {
    var showGlitch by remember { mutableStateOf(false) }
    var capyRainActive by remember { mutableStateOf(false) }
    var retroModeActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(if (retroModeActive) Color(0xFF001A00) else Color(0xFF0A0A0A))) {
        
        if (capyRainActive) {
            CapyRainOverlay()
        }
        
        if (retroModeActive) {
            ScanlineOverlay()
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CapyTopBar(
                    title = "CIA LABS",
                    onBackClick = onBackClick
                )
            }
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

                CIAFeatureCard(
                    title = "CapyVirus Simulation",
                    description = "Simulação visual de corrupção de sistema. 100% seguro.",
                    icon = Icons.Outlined.Warning,
                ) {
                    scope.launch {
                        showGlitch = true
                        delay(3000)
                        showGlitch = false
                    }
                }

                CIAFeatureCard(
                    title = "Capy Rain",
                    description = "Ativa precipitação atmosférica de capivaras digitais.",
                    icon = Icons.Outlined.CloudDownload,
                ) { 
                    capyRainActive = !capyRainActive 
                }

                CIAFeatureCard(
                    title = "Retro Mode",
                    description = "Habilita interface analógica de baixa resolução.",
                    icon = Icons.Outlined.SettingsBackupRestore,
                ) { 
                    retroModeActive = !retroModeActive 
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "CONNECTED TO MILHO_DATABASE_V4",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Green.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (showGlitch) {
            GlitchOverlay()
        }
    }
}

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
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.Green.copy(alpha = 0.3f))
    }
}

@Composable
private fun CIAFeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onActivate: () -> Unit
) {
    Surface(
        onClick = onActivate,
        color = Color.DarkGray.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Green.copy(alpha = 0.2f))
    ) {
        ListItem(
            headlineContent = { Text(title, color = Color.White, fontFamily = FontFamily.Monospace) },
            supportingContent = { Text(description, color = Color.LightGray, fontSize = 12.sp) },
            leadingContent = { Icon(icon, null, tint = Color.Green) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun GlitchOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "glitch")
    val offset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
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
        initialValue = -100f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "y"
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
