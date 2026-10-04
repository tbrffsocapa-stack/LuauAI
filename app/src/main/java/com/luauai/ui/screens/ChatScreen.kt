package com.luauai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luauai.data.models.ChatMessage
import com.luauai.data.models.SearchResult
import com.luauai.ui.components.LuauCodeBlock
import com.luauai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    projectId: Long,
    onOpenEditor: (Long) -> Unit,
    onOpenRules: () -> Unit,
    onBack: () -> Unit,
    vm: ChatViewModel = viewModel(
        key     = "chat_$projectId",
        factory = remember(projectId) {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ChatViewModel(projectId) as T
                }
            }
        }
    )
) {
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    // Rolar para o fim quando nova mensagem chegar
    LaunchedEffect(state.messages.size, state.streamingText) {
        if (state.messages.isNotEmpty() || state.streamingText.isNotEmpty()) {
            listState.animateScrollToItem(
                (state.messages.size + if (state.streamingText.isNotEmpty()) 1 else 0)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Luau AI", style = MaterialTheme.typography.titleMedium,
                            color = LuauPrimary)
                        if (state.statusMessage.isNotEmpty()) {
                            Text(state.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = LuauOnSurface)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = LuauOnSurface)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRules) {
                        Icon(Icons.Default.Rule, "Regras", tint = LuauSecondary)
                    }
                    IconButton(onClick = { vm.clearHistory() }) {
                        Icon(Icons.Default.Delete, "Limpar histórico", tint = LuauOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LuauSurface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text          = inputText,
                onTextChange  = { inputText = it },
                isGenerating  = state.isGenerating,
                onSend        = {
                    if (inputText.isNotBlank()) {
                        vm.sendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                onCancel      = { vm.cancelGeneration() }
            )
        },
        containerColor = LuauBackground
    ) { padding ->

        LazyColumn(
            state         = listState,
            modifier      = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Mensagem de boas-vindas
            if (state.messages.isEmpty() && !state.isGenerating) {
                item {
                    WelcomeCard()
                }
            }

            // Histórico de mensagens
            items(state.messages, key = { it.id }) { msg ->
                MessageBubble(message = msg, onOpenCode = { /* abrir editor */ })
            }

            // Resultados de pesquisa
            if (state.searchResults.isNotEmpty()) {
                item {
                    SearchResultsCard(results = state.searchResults)
                }
            }

            // Resposta em streaming
            if (state.streamingText.isNotEmpty()) {
                item {
                    StreamingBubble(text = state.streamingText)
                }
            }

            // Indicador de geração
            if (state.isGenerating && state.streamingText.isEmpty()) {
                item {
                    GeneratingIndicator(status = state.statusMessage)
                }
            }

            // Erro
            if (state.error != null) {
                item {
                    ErrorCard(message = state.error!!, onDismiss = { vm.clearError() })
                }
            }
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
private fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = LuauSurface),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌺 Luau AI", style = MaterialTheme.typography.titleLarge,
                color = LuauPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Especialista em Luau e Roblox.\nFaça perguntas, peça código, análises ou correções.",
                style = MaterialTheme.typography.bodyMedium,
                color = LuauOnSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            SuggestionChips()
        }
    }
}

@Composable
private fun SuggestionChips() {
    val suggestions = listOf(
        "Crie um sistema de ESP",
        "Crie um RemoteEvent de dano",
        "Explique OOP em Luau",
        "Corrija este script"
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { s ->
            AssistChip(
                onClick = { },
                label   = { Text(s, fontSize = 11.sp) },
                colors  = AssistChipDefaults.assistChipColors(
                    containerColor = LuauSurfaceVariant,
                    labelColor     = LuauOnSurface
                )
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onOpenCode: () -> Unit) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LuauPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🌺", fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            colors   = CardDefaults.cardColors(
                containerColor = if (isUser) LuauSecondary.copy(alpha = 0.3f) else LuauSurface
            ),
            shape = RoundedCornerShape(
                topStart    = if (isUser) 16.dp else 4.dp,
                topEnd      = if (isUser) 4.dp  else 16.dp,
                bottomStart = 16.dp,
                bottomEnd   = 16.dp
            )
        ) {
            if (!isUser) {
                // Parsear e renderizar conteúdo com blocos de código
                MessageContent(content = message.content)
            } else {
                Text(
                    text     = message.content,
                    modifier = Modifier.padding(12.dp),
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = LuauOnBackground
                )
            }
        }
    }
}

@Composable
private fun MessageContent(content: String) {
    val parts = parseMessageParts(content)
    Column(modifier = Modifier.padding(12.dp)) {
        parts.forEach { part ->
            when (part) {
                is MessagePart.Text -> {
                    SelectionContainer {
                        Text(
                            text  = part.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LuauOnBackground
                        )
                    }
                }
                is MessagePart.Code -> {
                    Spacer(Modifier.height(8.dp))
                    LuauCodeBlock(code = part.code, language = part.language)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

sealed class MessagePart {
    data class Text(val text: String) : MessagePart()
    data class Code(val code: String, val language: String) : MessagePart()
}

fun parseMessageParts(content: String): List<MessagePart> {
    val parts = mutableListOf<MessagePart>()
    val codeBlockRegex = Regex("```(\\w*)\\n([\\s\\S]*?)```")
    var lastEnd = 0

    codeBlockRegex.findAll(content).forEach { match ->
        if (match.range.first > lastEnd) {
            val text = content.substring(lastEnd, match.range.first).trim()
            if (text.isNotEmpty()) parts.add(MessagePart.Text(text))
        }
        val lang = match.groupValues[1].ifEmpty { "lua" }
        parts.add(MessagePart.Code(match.groupValues[2].trimEnd(), lang))
        lastEnd = match.range.last + 1
    }

    if (lastEnd < content.length) {
        val text = content.substring(lastEnd).trim()
        if (text.isNotEmpty()) parts.add(MessagePart.Text(text))
    }

    return parts
}

@Composable
private fun StreamingBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LuauPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("🌺", fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            colors   = CardDefaults.cardColors(containerColor = LuauSurface),
            shape    = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp)
        ) {
            MessageContent(content = text)
        }
    }
}

@Composable
private fun GeneratingIndicator(status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color    = LuauPrimary
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text  = status.ifEmpty { "Gerando resposta..." },
            style = MaterialTheme.typography.bodyMedium,
            color = LuauOnSurface
        )
    }
}

@Composable
private fun SearchResultsCard(results: List<SearchResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = LuauSurfaceVariant),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🔍 Fontes pesquisadas",
                style = MaterialTheme.typography.labelMedium,
                color = LuauPrimary)
            Spacer(Modifier.height(8.dp))
            results.take(3).forEach { r ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", color = LuauSecondary, modifier = Modifier.padding(end = 6.dp))
                    Column {
                        Text(r.title, style = MaterialTheme.typography.bodyMedium,
                            color = LuauOnBackground, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(r.url, style = MaterialTheme.typography.labelSmall,
                            color = LuauPrimary, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(r.snippet, style = MaterialTheme.typography.bodyMedium,
                            color = LuauOnSurface, maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = LuauError.copy(alpha = 0.15f)),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier            = Modifier.padding(12.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, null, tint = LuauError)
            Spacer(Modifier.width(8.dp))
            Text(message, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium, color = LuauError)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = LuauOnSurface)
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(color = LuauSurface, shadowElevation = 8.dp) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value         = text,
                onValueChange = onTextChange,
                modifier      = Modifier.weight(1f),
                placeholder   = {
                    Text("Peça código, análises, correções...",
                        color = LuauOnSurface.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium)
                },
                shape         = RoundedCornerShape(16.dp),
                minLines      = 1,
                maxLines      = 5,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = LuauPrimary,
                    unfocusedBorderColor = LuauSurfaceVariant,
                    cursorColor          = LuauPrimary,
                    focusedTextColor     = LuauOnBackground,
                    unfocusedTextColor   = LuauOnBackground,
                    focusedContainerColor   = LuauSurfaceVariant,
                    unfocusedContainerColor = LuauSurfaceVariant
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(8.dp))
            if (isGenerating) {
                IconButton(
                    onClick  = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LuauError.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Stop, "Cancelar", tint = LuauError)
                }
            } else {
                IconButton(
                    onClick  = onSend,
                    enabled  = text.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (text.isNotBlank()) LuauPrimary else LuauSurfaceVariant
                        )
                ) {
                    Icon(Icons.Default.Send, "Enviar",
                        tint = if (text.isNotBlank()) LuauOnPrimary else LuauOnSurface)
                }
            }
        }
    }
}
