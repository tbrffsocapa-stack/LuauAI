package com.luauai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luauai.LuauAIApp
import com.luauai.data.models.ProjectFile
import com.luauai.ui.components.highlightLuau
import com.luauai.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
data class EditorUiState(
    val file: ProjectFile? = null,
    val saved: Boolean     = true,
    val searchQuery: String = "",
    val searchVisible: Boolean = false,
    val replaceQuery: String = "",
    val replaceVisible: Boolean = false,
    val lineCount: Int = 1,
    val cursorLine: Int = 1,
    val cursorCol: Int = 1,
)

class EditorViewModel(
    private val projectId: Long,
    private val fileId: Long
) : ViewModel() {

    private val repo = LuauAIApp.instance.projectRepository
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    var textFieldValue by mutableStateOf(TextFieldValue(""))
        private set

    init {
        viewModelScope.launch {
            val file = repo.getFileById(fileId)
            if (file != null) {
                textFieldValue = TextFieldValue(file.content)
                _state.update { it.copy(
                    file      = file,
                    lineCount = file.content.lines().size
                )}
            }
        }
    }

    fun onTextChange(newVal: TextFieldValue) {
        textFieldValue = newVal
        val lines = newVal.text.lines()
        val pos   = newVal.selection.start
        val lineNum = newVal.text.substring(0, pos.coerceAtMost(newVal.text.length)).lines().size
        _state.update {
            it.copy(
                saved     = false,
                lineCount = lines.size,
                cursorLine = lineNum,
                cursorCol  = pos - newVal.text.lastIndexOf('\n', pos - 1)
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            val file = _state.value.file ?: return@launch
            repo.updateFile(file.copy(content = textFieldValue.text))
            _state.update { it.copy(saved = true) }
        }
    }

    fun toggleSearch()  = _state.update { it.copy(searchVisible  = !it.searchVisible) }
    fun toggleReplace() = _state.update { it.copy(replaceVisible = !it.replaceVisible) }

    fun onSearchChange(q: String) = _state.update { it.copy(searchQuery = q) }
    fun onReplaceChange(q: String) = _state.update { it.copy(replaceQuery = q) }

    fun findNext() {
        val q = _state.value.searchQuery.ifEmpty { return }
        val text = textFieldValue.text
        val start = textFieldValue.selection.end
        val idx = text.indexOf(q, start).takeIf { it >= 0 }
            ?: text.indexOf(q, 0)
        if (idx >= 0) {
            textFieldValue = textFieldValue.copy(
                selection = TextRange(idx, idx + q.length)
            )
        }
    }

    fun replaceNext() {
        val q = _state.value.searchQuery.ifEmpty { return }
        val r = _state.value.replaceQuery
        val text = textFieldValue.text
        val idx = text.indexOf(q)
        if (idx >= 0) {
            val newText = text.substring(0, idx) + r + text.substring(idx + q.length)
            textFieldValue = TextFieldValue(newText, TextRange(idx + r.length))
            _state.update { it.copy(saved = false) }
        }
    }

    fun replaceAll() {
        val q = _state.value.searchQuery.ifEmpty { return }
        val r = _state.value.replaceQuery
        val newText = textFieldValue.text.replace(q, r)
        textFieldValue = TextFieldValue(newText)
        _state.update { it.copy(saved = false) }
    }
}

// ── Tela ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: Long,
    fileId: Long,
    onBack: () -> Unit,
    vm: EditorViewModel = viewModel(
        key     = "editor_$fileId",
        factory = remember(projectId, fileId) {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : ViewModel> create(c: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return EditorViewModel(projectId, fileId) as T
                }
            }
        }
    )
) {
    val state by vm.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.file?.name ?: "Editor",
                            style = MaterialTheme.typography.titleMedium,
                            color = LuauOnBackground
                        )
                        Text(
                            "${state.lineCount} linhas • " +
                            "Ln ${state.cursorLine} Col ${state.cursorCol} • " +
                            if (state.saved) "Salvo ✓" else "Não salvo",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.saved) Color(0xFF6A9955) else LuauAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!state.saved) vm.save()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, null, tint = LuauOnSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleSearch() }) {
                        Icon(Icons.Default.Search, "Buscar", tint = LuauOnSurface)
                    }
                    IconButton(onClick = { vm.save() }) {
                        Icon(Icons.Default.Save, "Salvar",
                            tint = if (state.saved) LuauOnSurface else LuauPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LuauSurface)
            )
        },
        containerColor = Color(0xFF1E1E1E)
    ) { padding ->

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            // ── Barra de busca/substituição ──────────────────────────────────
            if (state.searchVisible) {
                SearchBar(
                    searchQuery   = state.searchQuery,
                    replaceQuery  = state.replaceQuery,
                    replaceVisible = state.replaceVisible,
                    onSearchChange  = vm::onSearchChange,
                    onReplaceChange = vm::onReplaceChange,
                    onFindNext      = vm::findNext,
                    onReplaceNext   = vm::replaceNext,
                    onReplaceAll    = vm::replaceAll,
                    onToggleReplace = vm::toggleReplace,
                    onClose         = vm::toggleSearch
                )
            }

            // ── Editor principal ─────────────────────────────────────────────
            val scrollState = rememberScrollState()
            val hScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Numeração de linhas
                Column(
                    modifier = Modifier
                        .background(Color(0xFF1A1A1A))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .widthIn(min = 40.dp)
                ) {
                    repeat(state.lineCount) { i ->
                        Text(
                            text  = (i + 1).toString(),
                            color = Color(0xFF858585),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMono,
                                fontSize   = 13.sp,
                                lineHeight = 20.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            modifier  = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Campo de texto
                BasicTextField(
                    value         = vm.textFieldValue,
                    onValueChange = vm::onTextChange,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .horizontalScroll(hScrollState),
                    textStyle     = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize   = 13.sp,
                        lineHeight = 20.sp,
                        color      = Color(0xFFD4D4D4)
                    ),
                    cursorBrush   = SolidColor(LuauPrimary),
                    visualTransformation = remember {
                        LuauHighlightTransformation()
                    }
                )
            }
        }
    }
}

// ── Transformação de texto para highlight em tempo real ──────────────────────
class LuauHighlightTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val highlighted = highlightLuau(text.text)
        return androidx.compose.ui.text.input.TransformedText(
            highlighted,
            androidx.compose.ui.text.input.OffsetMapping.Identity
        )
    }
}

// ── Barra de busca ────────────────────────────────────────────────────────────
@Composable
private fun SearchBar(
    searchQuery: String,
    replaceQuery: String,
    replaceVisible: Boolean,
    onSearchChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onReplaceNext: () -> Unit,
    onReplaceAll: () -> Unit,
    onToggleReplace: () -> Unit,
    onClose: () -> Unit
) {
    Surface(color = Color(0xFF2D2D2D)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = onSearchChange,
                    modifier      = Modifier.weight(1f).height(44.dp),
                    placeholder   = { Text("Buscar...", fontSize = 12.sp) },
                    singleLine    = true,
                    textStyle     = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp,
                        color = Color(0xFFD4D4D4)),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = LuauPrimary,
                        unfocusedBorderColor = Color(0xFF555555),
                        focusedTextColor     = Color(0xFFD4D4D4),
                        unfocusedTextColor   = Color(0xFFD4D4D4)
                    )
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onFindNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, "Próximo", tint = LuauOnSurface,
                        modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggleReplace, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FindReplace, "Substituir", tint = LuauOnSurface,
                        modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "Fechar", tint = LuauOnSurface,
                        modifier = Modifier.size(18.dp))
                }
            }

            if (replaceVisible) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value         = replaceQuery,
                        onValueChange = onReplaceChange,
                        modifier      = Modifier.weight(1f).height(44.dp),
                        placeholder   = { Text("Substituir por...", fontSize = 12.sp) },
                        singleLine    = true,
                        textStyle     = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp,
                            color = Color(0xFFD4D4D4)),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = LuauAccent,
                            unfocusedBorderColor = Color(0xFF555555),
                            focusedTextColor     = Color(0xFFD4D4D4),
                            unfocusedTextColor   = Color(0xFFD4D4D4)
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onReplaceNext) {
                        Text("1×", color = LuauAccent, fontSize = 12.sp)
                    }
                    TextButton(onClick = onReplaceAll) {
                        Text("Todos", color = LuauAccent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
