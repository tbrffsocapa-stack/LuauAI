package com.luauai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luauai.LuauAIApp
import com.luauai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onModelSetup: () -> Unit,
    onBack: () -> Unit
) {
    val engine = LuauAIApp.instance.llamaEngine

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações", color = LuauOnBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = LuauOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LuauSurface)
            )
        },
        containerColor = LuauBackground
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            SettingsSection("Modelo de IA") {
                SettingsItem(
                    icon    = Icons.Default.Memory,
                    title   = "Configurar Modelo",
                    subtitle = if (engine.loaded) engine.modelInfo else "Nenhum modelo carregado",
                    onClick = onModelSetup
                )
            }

            SettingsSection("Sobre") {
                SettingsItem(
                    icon    = Icons.Default.Info,
                    title   = "Luau AI",
                    subtitle = "v1.0.0 — IA local para Luau/Roblox"
                )
                SettingsItem(
                    icon    = Icons.Default.Code,
                    title   = "Motor",
                    subtitle = "llama.cpp via JNI — Modelo GGUF local"
                )
                SettingsItem(
                    icon    = Icons.Default.Shield,
                    title   = "Privacidade",
                    subtitle = "100% local. Nenhum dado enviado para servidores externos."
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title,
            style = MaterialTheme.typography.labelMedium,
            color = LuauPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = LuauSurface),
            shape  = RoundedCornerShape(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null)
        Modifier.fillMaxWidth().let { m ->
            m // clickable handled by Card
        }
    else Modifier.fillMaxWidth()

    if (onClick != null) {
        Card(
            onClick = onClick,
            colors  = CardDefaults.cardColors(containerColor = LuauSurface),
            shape   = RoundedCornerShape(0.dp)
        ) {
            SettingsItemContent(icon, title, subtitle, hasAction = true)
        }
    } else {
        SettingsItemContent(icon, title, subtitle, hasAction = false)
    }
}

@Composable
private fun SettingsItemContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    hasAction: Boolean
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = LuauSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = LuauOnBackground)
            if (subtitle.isNotEmpty()) {
                Text(subtitle,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = LuauOnSurface,
                    maxLines = 2)
            }
        }
        if (hasAction) {
            Icon(Icons.Default.ChevronRight, null,
                tint     = LuauOnSurface,
                modifier = Modifier.size(18.dp))
        }
    }
}
