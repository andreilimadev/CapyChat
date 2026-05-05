package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andreilima.capychat.ui.components.PrimaryButton

@Composable
fun HomeScreen(
    userName: String,
    onOpenConversations: () -> Unit,
    onOpenStatus: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Olá, $userName 👋", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Bem-vindo ao CapyChat")
        Spacer(modifier = Modifier.height(28.dp))

        PrimaryButton(text = "Entrar nas conversas", onClick = onOpenConversations)
        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(text = "Ver status", onClick = onOpenStatus)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sair")
        }
    }
}