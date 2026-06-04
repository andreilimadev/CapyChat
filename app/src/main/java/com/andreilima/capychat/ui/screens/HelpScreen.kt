package com.andreilima.capychat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andreilima.capychat.ui.components.CapyTopBar

@Composable
fun HelpScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CapyTopBar(
                title = "Ajuda",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Perguntas Frequentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FAQItem(
                question = "Como mudo minha tag única?",
                answer = "Atualmente as tags são permanentes para garantir a integridade das conversas. Em atualizações futuras permitiremos uma alteração por ano."
            )

            FAQItem(
                question = "O CapyChat é seguro?",
                answer = "Sim! Usamos criptografia do Firebase e regras rigorosas de banco de dados para garantir que apenas participantes leiam as mensagens."
            )

            FAQItem(
                question = "Como crio uma sala pública?",
                answer = "Toque no ícone de '+' na aba de conversas e selecione 'Nova Sala'. Ela ficará visível para todos os usuários."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                onClick = { /* Reportar */ },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.BugReport, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Reportar um Problema", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "CapyChat v1.0.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Feito com ❤️ pela Equipe Capy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(question, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(answer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
