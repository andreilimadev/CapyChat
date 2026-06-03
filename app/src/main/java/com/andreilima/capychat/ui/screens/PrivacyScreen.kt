package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
fun PrivacyScreen(
    onBackClick: () -> Unit,
    prefsViewModel: PreferencesViewModel = viewModel()
) {
    val showOnlineStatus by prefsViewModel.showOnlineStatus.collectAsStateWithLifecycle()
    val showReadReceipts by prefsViewModel.showReadReceipts.collectAsStateWithLifecycle()
    val showLastSeen     by prefsViewModel.showLastSeen.collectAsStateWithLifecycle()

    // allowDMs e showProfileByTag ainda não têm backend — ficam locais por ora
    var showProfileByTag by remember { mutableStateOf(true) }
    var allowDMs         by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { CapyTopBar(title = "Privacidade", onBackClick = onBackClick) }
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
                "Quem pode ver minhas informações",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PrivacySwitchRow(
                title = "Ver meu perfil pela tag",
                description = "Permite que outros encontrem você pesquisando sua tag única.",
                checked = showProfileByTag,
                onCheckedChange = { showProfileByTag = it }
            )
            PrivacySwitchRow(
                title = "Mostrar status online",
                description = "Se desativado, você também não verá o status de outros.",
                checked = showOnlineStatus,
                onCheckedChange = { prefsViewModel.setShowOnlineStatus(it) }
            )
            PrivacySwitchRow(
                title = "Mostrar último acesso",
                description = "Exibe quando você esteve online pela última vez.",
                checked = showLastSeen,
                onCheckedChange = { prefsViewModel.setShowLastSeen(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "Conversas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PrivacySwitchRow(
                title = "Permitir Mensagens Diretas",
                description = "Controla se desconhecidos podem iniciar chats com você.",
                checked = allowDMs,
                onCheckedChange = { allowDMs = it }
            )
            PrivacySwitchRow(
                title = "Confirmações de leitura",
                description = "Os famosos 'checks' de mensagem lida.",
                checked = showReadReceipts,
                onCheckedChange = { prefsViewModel.setShowReadReceipts(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Segurança",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                onClick = { },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                ListItem(
                    headlineContent = { Text("Contatos Bloqueados") },
                    supportingContent = { Text("0 usuários") },
                    leadingContent = { Icon(Icons.Outlined.Block, null) },
                    trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }
                )
            }
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}