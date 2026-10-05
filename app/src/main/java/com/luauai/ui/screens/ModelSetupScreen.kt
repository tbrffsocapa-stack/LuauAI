package com.luauai.ui.screens

import android.net.Uri
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import com.luauai.LuauAIApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "ModelSetup"
private val Context.dataStore by preferencesDataStore("luauai_prefs")

data class ModelSetupState(
    val modelPath: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isLoaded: Boolean = false,
    val loadResult: String? = null,
    val modelInfo: String = "",
    val contextSize: Int = 2048,
    val threads: Int = 4,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024
)

class ModelSetupViewModel : ViewModel() {

    private val app = LuauAIApp.instance
    private val engine = app.llamaEngine

    private val _state = MutableStateFlow(ModelSetupState())
    val state: StateFlow<ModelSetupState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            app.dataStore.data.collect { prefs ->
                val saved = prefs[MODEL_PATH_KEY] ?: ""

                if (saved.isNotEmpty()) {
                    val file = File(saved)

                    _state.update {
                        it.copy(
                            modelPath = saved,
                            isSaved = file.exists()
                        )
                    }
                }
            }
        }
    }

    fun setContextSize(value: Int) {
        _state.update { it.copy(contextSize = value) }
    }

    fun setThreads(value: Int) {
        _state.update { it.copy(threads = value) }
    }

    fun setTemperature(value: Float) {
        _state.update { it.copy(temperature = value) }
    }

    fun setMaxTokens(value: Int) {
        _state.update { it.copy(maxTokens = value) }
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            try {
                val resolver = app.contentResolver

                val name = resolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val index = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )

                    if (index >= 0 && cursor.moveToFirst()) {
                        cursor.getString(index)
                    } else {
                        null
                    }
                } ?: "model.gguf"

                if (!name.lowercase().endsWith(".gguf")) {
                    _state.update {
                        it.copy(
                            loadResult = "❌ Selecione um arquivo .gguf"
                        )
                    }
                    return@launch
                }

                val modelsDir = File(app.filesDir, "models")

                if (!modelsDir.exists()) {
                    modelsDir.mkdirs()
                }

                val target = File(modelsDir, name)

                _state.update {
                    it.copy(
                        isLoading = true,
                        loadResult = "📥 Importando modelo..."
                    )
                }

                resolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(
                            output,
                            1024 * 1024
                        )
                    }
                } ?: throw RuntimeException(
                    "Não foi possível abrir o arquivo selecionado."
                )

                if (!target.exists() || target.length() <= 0) {
                    throw RuntimeException(
                        "O arquivo foi copiado, mas ficou vazio."
                    )
                }

                _state.update {
                    it.copy(
                        modelPath = target.absolutePath,
                        isLoading = false,
                        isSaved = false,
                        isLoaded = false,
                        modelInfo = "",
                        loadResult =
                            "✅ Modelo importado!\n" +
                            "Tamanho: ${target.length() / (1024 * 1024)} MB"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao importar modelo", e)

                _state.update {
                    it.copy(
                        isLoading = false,
                        loadResult =
                            "❌ Erro ao importar:\n" +
                            "${e.javaClass.simpleName}: ${e.message}"
                    )
                }
            }
        }
    }

    fun saveModel() {
        viewModelScope.launch {
            try {
                val path = _state.value.modelPath.trim()

                if (path.isEmpty()) {
                    _state.update {
                        it.copy(
                            loadResult =
                                "❌ Nenhum modelo selecionado."
                        )
                    }
                    return@launch
                }

                val file = File(path)

                if (!file.exists()) {
                    _state.update {
                        it.copy(
                            isSaved = false,
                            loadResult =
                                "❌ O arquivo do modelo não existe."
                        )
                    }
                    return@launch
                }

                if (!file.name.lowercase().endsWith(".gguf")) {
                    _state.update {
                        it.copy(
                            isSaved = false,
                            loadResult =
                                "❌ O arquivo precisa ser .gguf."
                        )
                    }
                    return@launch
                }

                app.dataStore.edit { prefs ->
                    prefs[MODEL_PATH_KEY] = path
                }

                _state.update {
                    it.copy(
                        isSaved = true,
                        loadResult =
                            "💾 Modelo salvo!\n${file.name}"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar modelo", e)

                _state.update {
                    it.copy(
                        isSaved = false,
                        loadResult =
                            "❌ Erro ao salvar:\n" +
                            "${e.javaClass.simpleName}: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadModel() {
        viewModelScope.launch {
            try {
                val current = _state.value
                val path = current.modelPath.trim()

                if (path.isEmpty()) {
                    _state.update {
                        it.copy(
                            loadResult =
                                "❌ Nenhum modelo selecionado."
                        )
                    }
                    return@launch
                }

                val file = File(path)

                if (!file.exists()) {
                    _state.update {
                        it.copy(
                            isSaved = false,
                            isLoaded = false,
                            loadResult =
                                "❌ Modelo não encontrado:\n$path"
                        )
                    }
                    return@launch
                }

                if (file.length() <= 0) {
                    _state.update {
                        it.copy(
                            loadResult =
                                "❌ O arquivo do modelo está vazio."
                        )
                    }
                    return@launch
                }

                _state.update {
                    it.copy(
                        isLoading = true,
                        loadResult = "🧠 Carregando modelo..."
                    )
                }

                val ok = engine.load(
                    path,
                    current.contextSize,
                    current.threads
                )

                if (ok) {

                    app.dataStore.edit { prefs ->
                        prefs[MODEL_PATH_KEY] = path
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            isLoaded = true,
                            loadResult =
                                "✅ Modelo carregado com sucesso!",
                            modelInfo = engine.modelInfo
                        )
                    }

                } else {

                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoaded = false,
                            loadResult =
                                "❌ O llama.cpp não conseguiu carregar o GGUF."
                        )
                    }
                }

            } catch (e: Throwable) {
                Log.e(TAG, "Erro ao carregar modelo", e)

                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoaded = false,
                        loadResult =
                            "❌ ERRO AO CARREGAR:\n" +
                            "${e.javaClass.simpleName}: ${e.message}"
                    )
                }
            }
        }
    }

    fun unloadModel() {
        try {
            engine.free()

            _state.update {
                it.copy(
                    isLoaded = false,
                    modelInfo = "",
                    loadResult = "⏹️ Modelo descarregado."
                )
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao descarregar modelo", e)

            _state.update {
                it.copy(
                    loadResult =
                        "❌ Erro ao descarregar: ${e.message}"
                )
            }
        }
    }
}

private val MODEL_PATH_KEY =
    androidx.datastore.preferences.core.stringPreferencesKey(
        "model_path"
    )

@Composable
fun ModelSetupScreen(
    onContinue: () -> Unit,
    viewModel: ModelSetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                viewModel.importModel(uri)
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "🤖 Configurar LuauAI",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text =
                "Importe seu modelo GGUF, salve-o e carregue-o para usar a IA localmente.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "Modelo",
                    style = MaterialTheme.typography.titleLarge
                )

                if (state.modelPath.isNotEmpty()) {

                    Text(
                        text = "📦 ${File(state.modelPath).name}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = state.modelPath,
                        style = MaterialTheme.typography.bodySmall
                    )

                } else {

                    Text(
                        text = "Nenhum modelo selecionado.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        pickerLauncher.launch(
                            arrayOf("application/octet-stream", "*/*")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("📁 Selecionar modelo GGUF")
                }

                Button(
                    onClick = {
                        viewModel.saveModel()
                    },
                    enabled =
                        state.modelPath.isNotEmpty() &&
                        !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("💾 Salvar Modelo")
                }

                Button(
                    onClick = {
                        viewModel.loadModel()
                    },
                    enabled =
                        state.modelPath.isNotEmpty() &&
                        !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        if (state.isLoading)
                            "Carregando..."
                        else
                            "🧠 Carregar Modelo"
                    )
                }

                if (state.isLoaded) {

                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Continuar para o LuauAI")
                    }
                }

                if (state.isSaved) {
                    Text(
                        text = "💾 Salvo no armazenamento interno",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (state.isLoaded) {
                    Text(
                        text = "🟢 Modelo carregado",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "⚙️ Parâmetros",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Contexto: ${state.contextSize}"
                )

                Slider(
                    value = state.contextSize.toFloat(),
                    onValueChange = {
                        viewModel.setContextSize(
                            it.toInt()
                        )
                    },
                    valueRange = 512f..4096f,
                    steps = 6
                )

                Text(
                    text = "Threads: ${state.threads}"
                )

                Slider(
                    value = state.threads.toFloat(),
                    onValueChange = {
                        viewModel.setThreads(
                            it.toInt()
                        )
                    },
                    valueRange = 1f..8f,
                    steps = 6
                )

                Text(
                    text =
                        "Temperatura: %.2f"
                            .format(state.temperature)
                )

                Slider(
                    value = state.temperature,
                    onValueChange = {
                        viewModel.setTemperature(it)
                    },
                    valueRange = 0.1f..1.5f
                )

                Text(
                    text =
                        "Máximo de tokens: ${state.maxTokens}"
                )

                Slider(
                    value = state.maxTokens.toFloat(),
                    onValueChange = {
                        viewModel.setMaxTokens(
                            it.toInt()
                        )
                    },
                    valueRange = 128f..4096f,
                    steps = 30
                )
            }
        }

        state.modelInfo
            .takeIf { it.isNotBlank() }
            ?.let { info ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📊 Informações do modelo",
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            text = info,
                            fontSize = 13.sp
                        )
                    }
                }
            }

        state.loadResult
            ?.let { result ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp
                    )
                }
            }

        OutlinedButton(
            onClick = {
                viewModel.unloadModel()
            },
            enabled = state.isLoaded,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Stop,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text("Descarregar Modelo")
        }
    }
}
