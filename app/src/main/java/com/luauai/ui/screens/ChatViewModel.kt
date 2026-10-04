package com.luauai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luauai.LuauAIApp
import com.luauai.ai.engine.AIOrchestrator
import com.luauai.ai.engine.OrchestratorEvent
import com.luauai.data.models.ChatMessage
import com.luauai.data.models.Rule
import com.luauai.data.models.SearchResult
import com.luauai.data.repository.ChatRepository
import com.luauai.data.repository.RulesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage>    = emptyList(),
    val rules: List<Rule>              = emptyList(),
    val isGenerating: Boolean          = false,
    val statusMessage: String          = "",
    val streamingText: String          = "",
    val searchResults: List<SearchResult> = emptyList(),
    val error: String?                 = null
)

class ChatViewModel(
    private val projectId: Long,
    private val chatRepo: ChatRepository  = LuauAIApp.instance.chatRepository,
    private val rulesRepo: RulesRepository = LuauAIApp.instance.rulesRepository,
    private val orchestrator: AIOrchestrator = AIOrchestrator()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init {
        loadHistory()
        loadRules()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            chatRepo.getHistory(projectId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun loadRules() {
        viewModelScope.launch {
            rulesRepo.getProjectRules(projectId).collect { rules ->
                _uiState.update { it.copy(rules = rules) }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isGenerating) return

        viewModelScope.launch {
            // Salvar mensagem do usuário
            val userMsg = ChatMessage(
                projectId = projectId,
                content   = text,
                isUser    = true
            )
            chatRepo.addMessage(userMsg)

            // Buscar regras ativas (globais + do projeto)
            val activeRules = rulesRepo.getActiveRules(projectId)
            val history     = chatRepo.getHistorySync(projectId).takeLast(10)

            // Iniciar geração
            _uiState.update {
                it.copy(
                    isGenerating   = true,
                    streamingText  = "",
                    statusMessage  = "",
                    searchResults  = emptyList(),
                    error          = null
                )
            }

            generationJob = launch {
                val fullResponse = StringBuilder()

                orchestrator.process(
                    userMessage = text,
                    rules       = activeRules,
                    history     = history
                ).collect { event ->
                    when (event) {
                        is OrchestratorEvent.Status -> {
                            _uiState.update { it.copy(statusMessage = event.message) }
                        }
                        is OrchestratorEvent.SearchResults -> {
                            _uiState.update { it.copy(searchResults = event.results) }
                        }
                        is OrchestratorEvent.GenerationStart -> {
                            _uiState.update { it.copy(statusMessage = "") }
                        }
                        is OrchestratorEvent.Token -> {
                            fullResponse.append(event.text)
                            _uiState.update {
                                it.copy(streamingText = fullResponse.toString())
                            }
                        }
                        is OrchestratorEvent.GenerationEnd -> {
                            // Salvar resposta completa no banco
                            val aiMsg = ChatMessage(
                                projectId = projectId,
                                content   = fullResponse.toString(),
                                isUser    = false
                            )
                            chatRepo.addMessage(aiMsg)
                            _uiState.update {
                                it.copy(
                                    isGenerating  = false,
                                    streamingText = "",
                                    statusMessage = ""
                                )
                            }
                        }
                        is OrchestratorEvent.Error -> {
                            _uiState.update {
                                it.copy(
                                    isGenerating  = false,
                                    streamingText = "",
                                    error         = event.message
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        LuauAIApp.instance.llamaEngine.cancel()
        _uiState.update {
            it.copy(isGenerating = false, streamingText = "", statusMessage = "")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepo.clearHistory(projectId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
