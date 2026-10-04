package com.luauai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luauai.LuauAIApp
import com.luauai.data.models.Rule
import com.luauai.data.repository.RulesRepository
import com.luauai.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RulesViewModel(private val projectId: Long) : ViewModel() {
    private val repo: RulesRepository = LuauAIApp.instance.rulesRepository
    val rules: StateFlow<List<Rule>> = repo.getProjectRules(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRule(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repo.addRule(projectId, content, rules.value.size)
        }
    }

    fun toggleRule(rule: Rule) = viewModelScope.launch { repo.toggleRule(rule) }
    fun deleteRule(rule: Rule) = viewModelScope.launch { repo.deleteRule(rule) }
    fun updateRule(rule: Rule, newContent: String) = viewModelScope.launch {
        repo.updateRule(rule.copy(content = newContent))
    }
    fun insertDefaults() = viewModelScope.launch { repo.insertDefaults(projectId) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    projectId: Long,
    onBack: () -> Unit,
    vm: RulesViewModel = viewModel(
        key = "rules_$projectId",
        factory = remember(projectId) {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : ViewModel> create(c: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return RulesViewModel(projectId) as T
                }
            }
        }
    )
) {
    val rules by vm.rules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newRuleText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Regras da IA", style = MaterialTheme.typography.titleMedium,
                            color = LuauOnBackground)
                        Text("${rules.count { it.enabled }} ativas de ${rules.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LuauOnSurface)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = LuauOnSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.insertDefaults() }) {
                        Icon(Icons.Default.Restore, "Restaurar padrões", tint = LuauOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LuauSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { showAddDialog = true },
                containerColor   = LuauPrimary,
                contentColor     = LuauOnPrimary
            ) {
                Icon(Icons.Default.Add, "Adicionar regra")
            }
        },
        containerColor = LuauBackground
    ) { padding ->

        if (rules.isEmpty()) {
            Box(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentAlignment    = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nenhuma regra definida", color = LuauOnSurface,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { vm.insertDefaults() }) {
                        Text("Inserir regras padrão", color = LuauPrimary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    InfoBanner()
                }
                items(rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule      = rule,
                        onToggle  = { vm.toggleRule(it) },
                        onDelete  = { vm.deleteRule(it) },
                        onEdit    = { r, c -> vm.updateRule(r, c) }
                    )
                }
            }
        }
    }

    // ── Diálogo adicionar regra ───────────────────────────────────────────────
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newRuleText = "" },
            title   = { Text("Nova Regra", color = LuauOnBackground) },
            text    = {
                Column {
                    Text("Digite a instrução que a IA deve seguir:",
                        color = LuauOnSurface,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = newRuleText,
                        onValueChange = { newRuleText = it },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = {
                            Text("Ex: Responda sempre em português.",
                                color = LuauOnSurface.copy(alpha = 0.5f))
                        },
                        minLines = 2,
                        maxLines = 5,
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = LuauPrimary,
                            unfocusedBorderColor = LuauSurfaceVariant,
                            focusedTextColor     = LuauOnBackground,
                            unfocusedTextColor   = LuauOnBackground
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addRule(newRuleText.trim())
                    newRuleText = ""
                    showAddDialog = false
                }) {
                    Text("Adicionar", color = LuauPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newRuleText = "" }) {
                    Text("Cancelar", color = LuauOnSurface)
                }
            },
            containerColor = LuauSurface
        )
    }
}

@Composable
private fun InfoBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = LuauPrimary.copy(alpha = 0.1f)),
        shape  = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = LuauPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Estas regras são carregadas automaticamente em cada conversa deste projeto. " +
                "A IA seguirá suas instruções com prioridade máxima.",
                style = MaterialTheme.typography.bodyMedium,
                color = LuauOnSurface
            )
        }
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    onToggle: (Rule) -> Unit,
    onDelete: (Rule) -> Unit,
    onEdit: (Rule, String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var editText by remember(rule) { mutableStateOf(rule.content) }

    val bgColor by animateColorAsState(
        if (rule.enabled) LuauSurface else LuauSurface.copy(alpha = 0.5f),
        label = "ruleBg"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape  = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Toggle enable/disable
                Switch(
                    checked  = rule.enabled,
                    onCheckedChange = { onToggle(rule) },
                    colors   = SwitchDefaults.colors(
                        checkedThumbColor   = LuauPrimary,
                        checkedTrackColor   = LuauPrimary.copy(alpha = 0.3f),
                        uncheckedThumbColor = LuauOnSurface,
                        uncheckedTrackColor = LuauSurfaceVariant
                    ),
                    modifier = Modifier.size(width = 40.dp, height = 24.dp)
                )
                Spacer(Modifier.width(10.dp))

                if (editing) {
                    OutlinedTextField(
                        value         = editText,
                        onValueChange = { editText = it },
                        modifier      = Modifier.weight(1f),
                        minLines      = 2,
                        maxLines      = 5,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = LuauPrimary,
                            unfocusedBorderColor = LuauSurfaceVariant,
                            focusedTextColor     = LuauOnBackground,
                            unfocusedTextColor   = LuauOnBackground
                        )
                    )
                } else {
                    Text(
                        text      = rule.content,
                        modifier  = Modifier.weight(1f),
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = if (rule.enabled) LuauOnBackground else LuauOnSurface
                    )
                }

                Spacer(Modifier.width(4.dp))
                Column {
                    if (editing) {
                        IconButton(onClick = {
                            onEdit(rule, editText)
                            editing = false
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Check, "Salvar",
                                tint = LuauPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = {
                            editText = rule.content
                            editing  = false
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Cancelar",
                                tint = LuauOnSurface, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        IconButton(onClick = { editing = true },
                            modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, "Editar",
                                tint = LuauOnSurface, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDelete(rule) },
                            modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Deletar",
                                tint = LuauError, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
