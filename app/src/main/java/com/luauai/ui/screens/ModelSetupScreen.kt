package com.luauai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.compose.foundation.BorderStroke

import androidx.compose.ui.graphics.Color

import android.os.Environment
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
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import com.luauai.LuauAIApp
import com.luauai.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private val Context.dataStore by preferencesDataStore("luauai_prefs")
private val MODEL_PATH_KEY = stringPreferencesKey("model_path")

data class ModelSetupState(
    val modelPath: String      = "",
    val isLoading: Boolean     = false,
    val loadResult: String?    = null,
    val modelInfo: String      = "",
    val contextSize: Int       = 2048,
    val threads: Int           = 4,
    val temperature: Float     = 0.7f,
    val maxTokens: Int         = 1024,
    val availableFiles: List<String> = emptyList()
)

class ModelSetupViewModel : ViewModel() {
    private val app    = LuauAIApp.instance
    private val engine = app.llamaEngine
    private val _state = MutableStateFlow(ModelSetupState())
    val state: StateFlow<ModelSetupState> = _state.asStateFlow()

    init {
        // Carregar caminho salvo
        viewModelScope.launch {
            app.dataStore.data.collect { prefs ->
                val saved = prefs[MODEL_PATH_KEY] ?: ""
                _state.update { it.copy(modelPath = saved) }
            }
        }
        scanForModels()
    }

    /** Procura arquivos .gguf nos diretórios comuns do dispositivo */
    private fun scanForModels() {
        viewModelScope.launch {
            val dirs = listOf(
                Environment.getExternalStorageDirectory().absolutePath,
                "${Environment.getExternalStorageDirectory()}/Download",
                "${Environment.getExternalStorageDirectory()}/Models",
                app.filesDir.absolutePath,
                "${app.filesDir}/models"
            )
            val found = mutableListOf<String>()
            dirs.forEach { dir ->
                try {
                    File(dir).walkTopDown().maxDepth(3).forEach { f ->
                        if (f.extension == "gguf" && f.isFile) {
                            found.add(f.absolutePath)
                        }
                    }
                } catch (_: Exception) {}
            }
            _state.update { it.copy(availableFiles = found) }
        }
    }

    fun setModelPath(path: String) = _state.update { it.copy(modelPath = path) }
    fun setContextSize(v: Int)     = _state.update { it.copy(contextSize = v) }
    fun setThreads(v: Int)         = _state.update { it.copy(threads = v) }
    fun setTemperature(v: Float)   = _state.update { it.copy(temperature = v) }
    fun setMaxTokens(v: Int)       = _state.update { it.copy(maxTokens = v) }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            try {
                val resolver = app.contentResolver

                val name = resolver.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (i >= 0 && c.moveToFirst()) c.getString(i) else null
                } ?: "model.gguf"

                if (!name.lowercase().endsWith(".gguf")) {
                    _state.update {
                        it.copy(loadResult = "❌ Selecione um arquivo .gguf")
                    }
                    return@launch
                }

                val dir = File(app.filesDir, "models")
                dir.mkdirs()

                val target = File(dir, name)

                resolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output, 1024 * 1024)
                    }
                } ?: throw RuntimeException("Não foi possível abrir o arquivo")

                _state.update {
                    it.copy(
                        modelPath = target.absolutePath,
                        loadResult = "✅ Modelo importado: $name"
                    )
                }

                app.dataStore.edit { prefs ->
                    prefs[MODEL_PATH_KEY] = target.absolutePath
                }

            } catch (e: Exception) {
                android.util.Log.e("ModelSetup", "Erro ao importar modelo", e)

                _state.update {
                    it.copy(
                        loadResult =
                            "❌ Erro ao importar: ${e.javaClass.simpleName}: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadModel() {
        viewModelScope.launch {
            val path = _state.value.modelPath.trim()
            if (path.isEmpty()) {
                _state.update { it.copy(loadResult = "❌ Caminho do modelo não definido") }
                return@launch
            }
            _state.update { it.copy(isLoading = true, loadResult = null) }
            val ok = try {
                engine.load(path, _state.value.contextSize, _state.value.threads)
            } catch (e: Exception) {
                android.util.Log.e("ModelSetup", "Erro ao carregar modelo", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadResult = "❌ ERRO: ${e.javaClass.simpleName}: ${e.message}"
                    )
                }
                return@launch
            }
            if (ok) {
                // Salvar caminho
                app.dataStore.edit { prefs -> prefs[MODEL_PATH_KEY] = path }
                _state.update {
                    it.copy(
                        isLoading  = false,
                        loadResult = "✅ Modelo carregado!",
                        modelInfo  = engine.modelInfo
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading  = false,
                        loadResult = "❌ Falha ao carregar. Verifique o caminho e o arquivo GGUF."
                    )
                }
            }
        }
    }

    fun unloadModel() {
        engine.free()
        _state.update { it.copy(modelInfo = "", loadResult = "Modelo descarregado.") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSetupScreen(
    onBack: () -> Unit,
    vm: ModelSetupViewModel = viewModel()
) {
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importModel(uri)
    }


    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Modelo", color = LuauOnBackground) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Aviso importante ─────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = LuauSecondary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = LuauSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Modelo GGUF Local",
                            style = MaterialTheme.typography.titleMedium,
                            color = LuauSecondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Coloque um arquivo .gguf no armazenamento do seu dispositivo.\n\n" +
                        "Modelos recomendados (baixe no HuggingFace):\n" +
                        "• Llama-3.2-1B-Instruct-Q4_K_M.gguf (~800MB)\n" +
                        "• Phi-3-mini-4k-instruct-q4.gguf (~2.2GB)\n" +
                        "• Gemma-2-2b-it-Q4_K_M.gguf (~1.5GB)\n\n" +
                        "⚠️ Modelos maiores são mais lentos mas mais precisos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LuauOnSurface
                    )
                }
            }

            // ── Arquivos encontrados ──────────────────────────────────────────
            if (state.availableFiles.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LuauSurface),
                    shape  = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Arquivos encontrados",
                            style = MaterialTheme.typography.titleMedium,
                            color = LuauOnBackground)
                        Spacer(Modifier.height(10.dp))
                        state.availableFiles.forEach { path ->
                            Card(
                                onClick = { vm.setModelPath(path) },
                                colors  = CardDefaults.cardColors(
                                    containerColor = if (state.modelPath == path)
                                        LuauPrimary.copy(alpha = 0.15f)
                                    else
                                        LuauSurfaceVariant
                                ),
                                shape   = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storage, null,
                                        tint     = if (state.modelPath == path) LuauPrimary else LuauOnSurface,
                                        modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        path.substringAfterLast("/"),
                                        style    = MaterialTheme.typography.labelMedium,
                                        color    = if (state.modelPath == path) LuauPrimary else LuauOnBackground,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Caminho manual ───────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = LuauSurface),
                shape  = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Caminho do Modelo",
                        style = MaterialTheme.typography.titleMedium,
                        color = LuauOnBackground)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value         = state.modelPath,
                        onValueChange = vm::setModelPath,
                        modifier      = Modifier.fillMaxWidth(),
                        label         = { Text("Caminho completo do arquivo .gguf") },
                        placeholder   = {
                            Text("/storage/emulated/0/Download/modelo.gguf",
                                color = LuauOnSurface.copy(alpha = 0.4f), fontSize = 11.sp)
                        },
                        singleLine = true,
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = LuauPrimary,
                            unfocusedBorderColor = LuauSurfaceVariant,
                            focusedTextColor     = LuauOnBackground,
                            unfocusedTextColor   = LuauOnBackground,
                            focusedLabelColor    = LuauPrimary
                        )
                    )
                }
            }

            // ── Parâmetros ───────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = LuauSurface),
                shape  = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Parâmetros", style = MaterialTheme.typography.titleMedium,
                        color = LuauOnBackground)
                    Spacer(Modifier.height(12.dp))

                    SliderParam(
                        label    = "Contexto: ${state.contextSize} tokens",
                        value    = state.contextSize.toFloat(),
                        range    = 512f..4096f,
                        steps    = 6,
                        onChange = { vm.setContextSize(it.toInt()) }
                    )
                    SliderParam(
                        label    = "Threads: ${state.threads}",
                        value    = state.threads.toFloat(),
                        range    = 1f..8f,
                        steps    = 6,
                        onChange = { vm.setThreads(it.toInt()) }
                    )
                    SliderParam(
                        label    = "Temperatura: ${"%.1f".format(state.temperature)}",
                        value    = state.temperature,
                        range    = 0.1f..1.5f,
                        steps    = 13,
                        onChange = { vm.setTemperature(it) }
                    )
                    SliderParam(
                        label    = "Máx. tokens: ${state.maxTokens}",
                        value    = state.maxTokens.toFloat(),
                        range    = 128f..2048f,
                        steps    = 14,
                        onChange = { vm.setMaxTokens(it.toInt()) }
                    )
                }
            }

            // ── Resultado ────────────────────────────────────────────────────
            if (state.loadResult != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.loadResult!!.startsWith("✅"))
                            Color(0xFF1A2E1A) else Color(0xFF2E1A1A)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(state.loadResult!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.loadResult!!.startsWith("✅"))
                                Color(0xFF6A9955) else LuauError)
                        if (state.modelInfo.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(state.modelInfo,
                                style = MaterialTheme.typography.labelSmall,
                                color = LuauOnSurface)
                        }
                    }
                }
            }

            // ── Botões ───────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                    onClick = {
                        filePicker.launch(
                            arrayOf("application/octet-stream", "*/*")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📁 Selecionar modelo GGUF")
                }

Button(
                    onClick  = { vm.loadModel() },
                    enabled  = !state.isLoading && state.modelPath.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = LuauPrimary,
                        contentColor   = LuauOnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp, color = LuauOnPrimary)
                    } else {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isLoading) "Carregando..." else "Carregar Modelo")
                }
                OutlinedButton(
                    onClick = { vm.unloadModel() },
                    shape   = RoundedCornerShape(12.dp),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = LuauError),
                    border  = BorderStroke(1.dp, LuauError.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Stop, null, Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SliderParam(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LuauOnSurface)
        Slider(
            value         = value,
            onValueChange = onChange,
            valueRange    = range,
            steps         = steps,
            colors        = SliderDefaults.colors(
                thumbColor        = LuauPrimary,
                activeTrackColor  = LuauPrimary,
                inactiveTrackColor = LuauSurfaceVariant
            )
        )
    }
}
