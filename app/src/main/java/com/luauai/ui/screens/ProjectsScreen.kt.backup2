package com.luauai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luauai.LuauAIApp
import com.luauai.data.models.Project
import com.luauai.data.repository.ProjectRepository
import com.luauai.data.repository.RulesRepository
import com.luauai.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProjectsViewModel : ViewModel() {
    private val repo: ProjectRepository  = LuauAIApp.instance.projectRepository
    private val rulesRepo: RulesRepository = LuauAIApp.instance.rulesRepository

    val projects: StateFlow<List<Project>> = repo.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProject(name: String, description: String): Long {
        var id = 0L
        viewModelScope.launch {
            id = repo.create(name, description)
            // Inserir regras padrão
            rulesRepo.insertDefaults(id)
            // Criar arquivo inicial
            repo.createFile(id, "Main.server.lua", "-- Script principal\nlocal Players = game:GetService(\"Players\")\n\n")
        }
        return id
    }

    fun deleteProject(project: Project) = viewModelScope.launch { repo.delete(project) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onOpenProject: (Long) -> Unit,
    onBack: () -> Unit,
    vm: ProjectsViewModel = viewModel()
) {
    val projects by vm.projects.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Projetos", style = MaterialTheme.typography.titleMedium,
                        color = LuauOnBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = LuauOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LuauSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showCreateDialog = true },
                containerColor = LuauPrimary,
                contentColor   = LuauOnPrimary
            ) {
                Icon(Icons.Default.Add, "Novo projeto")
            }
        },
        containerColor = LuauBackground
    ) { padding ->

        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nenhum projeto ainda", color = LuauOnSurface)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showCreateDialog = true }) {
                        Text("Criar primeiro projeto", color = LuauPrimary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project   = project,
                        onClick   = { onOpenProject(project.id) },
                        onDelete  = { vm.deleteProject(project) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newName = ""; newDesc = "" },
            title = { Text("Novo Projeto", color = LuauOnBackground) },
            text  = {
                Column {
                    OutlinedTextField(
                        value         = newName,
                        onValueChange = { newName = it },
                        label         = { Text("Nome do projeto") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = LuauPrimary,
                            unfocusedBorderColor = LuauSurfaceVariant,
                            focusedTextColor     = LuauOnBackground,
                            unfocusedTextColor   = LuauOnBackground,
                            focusedLabelColor    = LuauPrimary
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = newDesc,
                        onValueChange = { newDesc = it },
                        label         = { Text("Descrição (opcional)") },
                        maxLines      = 3,
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = LuauPrimary,
                            unfocusedBorderColor = LuauSurfaceVariant,
                            focusedTextColor     = LuauOnBackground,
                            unfocusedTextColor   = LuauOnBackground,
                            focusedLabelColor    = LuauPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        if (newName.isNotBlank()) {
                            vm.createProject(newName.trim(), newDesc.trim())
                            showCreateDialog = false
                            newName = ""; newDesc = ""
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Criar", color = LuauPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar", color = LuauOnSurface)
                }
            },
            containerColor = LuauSurface
        )
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors  = CardDefaults.cardColors(containerColor = LuauSurface),
        shape   = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(end = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("📁", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = LuauOnBackground)
                if (project.description.isNotEmpty()) {
                    Text(project.description,
                        style  = MaterialTheme.typography.bodyMedium,
                        color  = LuauOnSurface,
                        maxLines = 1)
                }
                Text(
                    "Atualizado em ${fmt.format(Date(project.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LuauOnSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, null, tint = LuauError)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Deletar projeto?", color = LuauOnBackground) },
            text  = { Text("Isso apagará o projeto e todos os seus arquivos e histórico.",
                color = LuauOnSurface) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Deletar", color = LuauError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = LuauOnSurface)
                }
            },
            containerColor = LuauSurface
        )
    }
}
