package com.luauai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luauai.LuauAIApp
import com.luauai.data.models.Project
import com.luauai.data.repository.ProjectRepository
import com.luauai.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repo: ProjectRepository = LuauAIApp.instance.projectRepository
    val recentProjects: StateFlow<List<Project>> = repo.getAllProjects()
        .map { it.take(3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modelLoaded: Boolean
        get() = LuauAIApp.instance.llamaEngine.loaded

    val modelInfo: String
        get() = LuauAIApp.instance.llamaEngine.modelInfo

    fun createQuickProject(name: String): Long {
        var id = 0L
        viewModelScope.launch {
            id = repo.create(name)
            LuauAIApp.instance.rulesRepository.insertDefaults(id)
        }
        return id
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: (Long) -> Unit,
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onModelSetup: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val recentProjects by vm.recentProjects.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌺", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Luau AI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = LuauPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Configurações", tint = LuauOnSurface)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Status do modelo ─────────────────────────────────────────────
            ModelStatusCard(
                loaded    = vm.modelLoaded,
                info      = vm.modelInfo,
                onSetup   = onModelSetup
            )

            // ── Início rápido ────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = LuauSurface),
                shape  = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Início Rápido",
                        style = MaterialTheme.typography.titleMedium,
                        color = LuauOnBackground)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            // Criar projeto rápido e abrir chat
                            val id = vm.createQuickProject("Novo Projeto")
                            // Pequeno delay para garantir criação
                            onOpenChat(id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = LuauPrimary,
                            contentColor   = LuauOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nova conversa Luau", fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick  = onOpenProjects,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = LuauPrimary
                        ),
                        border = BorderStroke(1.dp, LuauPrimary.copy(alpha = 0.5f)),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gerenciar projetos")
                    }
                }
            }

            // ── Projetos recentes ────────────────────────────────────────────
            if (recentProjects.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LuauSurface),
                    shape  = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("Projetos Recentes",
                                style = MaterialTheme.typography.titleMedium,
                                color = LuauOnBackground)
                            TextButton(onClick = onOpenProjects) {
                                Text("Ver todos", color = LuauPrimary, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        recentProjects.forEach { project ->
                            RecentProjectItem(
                                project = project,
                                onClick = { onOpenChat(project.id) }
                            )
                            if (project != recentProjects.last()) {
                                Divider(color = LuauSurfaceVariant, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ── Recursos ─────────────────────────────────────────────────────
            FeatureGrid()

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModelStatusCard(loaded: Boolean, info: String, onSetup: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (loaded)
                Color(0xFF1A2E1A)
            else
                Color(0xFF2E1A1A)
        ),
        shape  = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (loaded) Color(0xFF6A9955).copy(0.2f)
                        else LuauError.copy(0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (loaded) Icons.Default.CheckCircle else Icons.Default.Warning,
                    null,
                    tint = if (loaded) Color(0xFF6A9955) else LuauError
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (loaded) "Modelo carregado" else "Modelo não configurado",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (loaded) Color(0xFF6A9955) else LuauError
                )
                Text(
                    if (loaded) info else "Configure um modelo GGUF para usar a IA",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = LuauOnSurface,
                    maxLines = 1
                )
            }
            if (!loaded) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onSetup,
                    colors  = ButtonDefaults.filledTonalButtonColors(
                        containerColor = LuauError.copy(alpha = 0.2f),
                        contentColor   = LuauError
                    )
                ) {
                    Text("Configurar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RecentProjectItem(project: Project, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📁", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(project.name,
                style = MaterialTheme.typography.bodyMedium,
                color = LuauOnBackground)
            if (project.description.isNotEmpty()) {
                Text(project.description,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = LuauOnSurface,
                    maxLines = 1)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = LuauOnSurface, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun FeatureGrid() {
    val features = listOf(
        Triple("🤖", "IA Local", "Roda 100% no seu dispositivo"),
        Triple("📝", "Editor Luau", "Highlight, busca e substituição"),
        Triple("🔍", "Pesquisa Web", "Consulta documentação automaticamente"),
        Triple("📋", "Regras", "Você controla o comportamento da IA"),
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = LuauSurface),
        shape  = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recursos", style = MaterialTheme.typography.titleMedium,
                color = LuauOnBackground)
            Spacer(Modifier.height(12.dp))
            features.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (emoji, title, desc) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors   = CardDefaults.cardColors(
                                containerColor = LuauSurfaceVariant),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(emoji, fontSize = 22.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LuauOnBackground)
                                Text(desc,
                                    style    = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color    = LuauOnSurface,
                                    maxLines = 2)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
