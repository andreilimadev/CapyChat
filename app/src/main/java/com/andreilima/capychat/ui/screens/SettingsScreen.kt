package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andreilima.capychat.ui.components.CapyTopBar
import com.andreilima.capychat.ui.viewmodel.PreferencesViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    prefsViewModel: PreferencesViewModel = viewModel()
) {
    val darkTheme           by prefsViewModel.darkTheme.collectAsStateWithLifecycle()
    val reduceAnimations    by prefsViewModel.reduceAnimations.collectAsStateWithLifecycle()
    val vibrationEnabled    by prefsViewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val soundsEnabled       by prefsViewModel.soundsEnabled.collectAsStateWithLifecycle()
    val experimentalEnabled by prefsViewModel.experimentalEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { CapyTopBar(title = "Configurações", onBackClick = onBackClick) }
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
                onCheckedChange = { prefsViewModel.setDarkTheme(it) }
            )
            SettingSwitchRow(
                title = "Reduzir animações",
                icon = Icons.Outlined.Animation,
                checked = reduceAnimations,
                onCheckedChange = { prefsViewModel.setReduceAnimations(it) }
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
                onCheckedChange = { prefsViewModel.setVibrationEnabled(it) }
            )
            SettingSwitchRow(
                title = "Sons do sistema",
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                checked = soundsEnabled,
                onCheckedChange = { prefsViewModel.setSoundsEnabled(it) }
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
                checked = experimentalEnabled,
                onCheckedChange = { prefsViewModel.setExperimentalEnabled(it) }
            )

            if (experimentalEnabled) {
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
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
        )
    }
}