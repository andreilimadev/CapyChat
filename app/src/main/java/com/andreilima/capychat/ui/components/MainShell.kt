package com.andreilima.capychat.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.andreilima.capychat.Screen

@Composable
fun MainShell(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    snackbarHostState: SnackbarHostState,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.CONVERSATIONS,
                    onClick = { if (currentScreen != Screen.CONVERSATIONS) onNavigate(Screen.CONVERSATIONS) },
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, null) },
                    label = { Text("Conversas") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.STATUS,
                    onClick = { if (currentScreen != Screen.STATUS) onNavigate(Screen.STATUS) },
                    icon = { Icon(Icons.Outlined.AutoStories, null) },
                    label = { Text("Status") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.PROFILE,
                    onClick = { if (currentScreen != Screen.PROFILE) onNavigate(Screen.PROFILE) },
                    icon = { Icon(Icons.Outlined.AccountCircle, null) },
                    label = { Text("Perfil") }
                )
            }
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        content(padding)
    }
}
