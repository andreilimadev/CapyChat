package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andreilima.capychat.ui.components.CapyTopBar

@Composable
fun SettingsScreen(
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    var reduceAnimations by rememberSaveable { mutableStateOf(false) }
    var vibrationEnabled by rememberSaveable { mutableStateOf(true) }
    var soundsEnabled by rememberSaveable { mutableStateOf(true) }
    var experimentalFeatures by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CapyTopBar(
                title = "Configurações",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Aparência",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingSwitchRow(
                title = "Modo Escuro",
                icon = Icons.Outlined.DarkMode,
                checked = darkTheme,
                onCheckedChange = onThemeChange
            )

            SettingSwitchRow(
                title = "Reduzir animações",
                icon = Icons.Outlined.Animation,
                checked = reduceAnimations,
                onCheckedChange = { reduceAnimations = it }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "Sons e Vibração",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingSwitchRow(
                title = "Vibração",
                icon = Icons.Outlined.Vibration,
                checked = vibrationEnabled,
                onCheckedChange = { vibrationEnabled = it }
            )

            SettingSwitchRow(
                title = "Sons do sistema",
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                checked = soundsEnabled,
                onCheckedChange = { soundsEnabled = it }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "Capy Labs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingSwitchRow(
                title = "Recursos Experimentais",
                icon = Icons.Outlined.Science,
                checked = experimentalFeatures,
                onCheckedChange = { experimentalFeatures = it }
            )
            
            if (experimentalFeatures) {
                Text(
                    "CUIDADO: Estes recursos podem ser instáveis e são apenas para testes internos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        ListItem(
            headlineContent = { Text(title) },
            leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
        )
    }
}
