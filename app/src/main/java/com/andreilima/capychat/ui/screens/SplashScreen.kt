package com.andreilima.capychat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    // 0f = olho aberto, 1f = olho fechado
    val blinkAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Entrada com bounce
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(1f, animationSpec = tween(400))
        delay(200)
        subtitleAlpha.animateTo(1f, animationSpec = tween(600))

        // 1ª piscada
        delay(800)
        blinkAlpha.animateTo(1f, animationSpec = tween(60))   // fecha rápido
        delay(180)
        blinkAlpha.animateTo(0f, animationSpec = tween(80))   // abre

        // 2ª piscada
        delay(1000)
        blinkAlpha.animateTo(1f, animationSpec = tween(60))
        delay(180)
        blinkAlpha.animateTo(0f, animationSpec = tween(80))

        delay(900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF2E0C8), Color(0xFFE8D0B0))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale.value),
                contentAlignment = Alignment.Center
            ) {
                // Frame 1 — olho aberto (base)
                Image(
                    painter = painterResource(id = R.drawable.capy_logo),
                    contentDescription = "CapyChat",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .alpha(1f - blinkAlpha.value),
                    contentScale = ContentScale.Crop
                )

                // Frame 2 — olho fechado (sobrepõe com fade)
                Image(
                    painter = painterResource(id = R.drawable.capy_blink),
                    contentDescription = null,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .alpha(blinkAlpha.value),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "CapyChat",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C3D1E),
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "conecte-se com a matilha 🐾",
                fontSize = 14.sp,
                color = Color(0xFF8B6340),
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}