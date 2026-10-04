package com.luauai.ai.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.channels.awaitClose
import java.io.File

/**
 * LlamaEngine — ponte Kotlin para o llama.cpp via JNI.
 *
 * O modelo GGUF fica armazenado localmente no dispositivo (pasta /models/).
 * Toda a inferência ocorre on-device, sem nenhuma chamada de rede para IA externa.
 */
class LlamaEngine {

    companion object {
        private const val TAG = "LlamaEngine"

        init {
            try {
                System.loadLibrary("luauai_jni")
                Log.i(TAG, "Biblioteca nativa carregada com sucesso")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Falha ao carregar biblioteca nativa: ${e.message}")
            }
        }
    }

    // ── JNI declarations ────────────────────────────────────────────────────
    private external fun loadModel(modelPath: String, nCtx: Int, nThreads: Int): Boolean
    private external fun isModelLoaded(): Boolean
    private external fun freeModel()
    private external fun resetContext()
    private external fun cancelGeneration()
    private external fun generate(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        tokenCallback: TokenCallback?
    ): String

    private external fun nativeGetModelInfo(): String

    // ── Interface de callback de token ───────────────────────────────────────
    interface TokenCallback {
        fun onToken(token: String)
    }

    // ── Estado público ───────────────────────────────────────────────────────
    var modelPath: String? = null
        private set

    val loaded: Boolean get() = isModelLoaded()
    val modelInfo: String get() = if (loaded) nativeGetModelInfo() else "Nenhum modelo carregado"

    // ── Carregar modelo GGUF ─────────────────────────────────────────────────
    fun load(path: String, nCtx: Int = 2048, nThreads: Int = 4): Boolean {
        val file = File(path)
        if (!file.exists()) {
            Log.e(TAG, "Arquivo de modelo não encontrado: $path")
            return false
        }
        val ok = loadModel(path, nCtx, nThreads)
        if (ok) modelPath = path
        return ok
    }

    // ── Gerar resposta como Flow (streaming token a token) ───────────────────
    fun generateFlow(
        prompt: String,
        maxTokens: Int   = 1024,
        temperature: Float = 0.7f,
        topP: Float      = 0.9f
    ): Flow<String> = callbackFlow {
        val cb = object : TokenCallback {
            override fun onToken(token: String) {
                trySend(token)
            }
        }
        generate(prompt, maxTokens, temperature, topP, cb)
        close()
        awaitClose { /* nada */ }
    }.flowOn(Dispatchers.Default)

    // ── Gerar resposta completa (bloqueante) ─────────────────────────────────
    suspend fun generateBlocking(
        prompt: String,
        maxTokens: Int    = 1024,
        temperature: Float = 0.7f,
        topP: Float       = 0.9f
    ): String = kotlinx.coroutines.withContext(Dispatchers.Default) {
        generate(prompt, maxTokens, temperature, topP, null)
    }

    fun reset()  = resetContext()
    fun cancel() = cancelGeneration()
    fun free()   = freeModel()
}
