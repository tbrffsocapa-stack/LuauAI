package com.luauai.ai.prompt

import com.luauai.data.models.ChatMessage
import com.luauai.data.models.Rule

object PromptBuilder {

    enum class CodeOperation {
        CREATE,
        MODIFY,
        FIX,
        EXPLAIN,
        GENERATE,
        UNKNOWN
    }

    fun detectCodeOperation(message: String): CodeOperation {
        val text = message.lowercase()

        return when {
            listOf(
                "corrija",
                "corrigir",
                "conserte",
                "consertar",
                "fix",
                "erro",
                "bug"
            ).any { text.contains(it) } -> CodeOperation.FIX

            listOf(
                "modifique",
                "modificar",
                "altere",
                "alterar",
                "mude",
                "editar",
                "edite"
            ).any { text.contains(it) } -> CodeOperation.MODIFY

            listOf(
                "crie",
                "criar",
                "faça",
                "faca",
                "fazer",
                "gere",
                "gerar",
                "implemente",
                "implementar"
            ).any { text.contains(it) } -> CodeOperation.CREATE

            listOf(
                "explique",
                "explica",
                "explicar",
                "como funciona",
                "o que é",
                "o que e"
            ).any { text.contains(it) } -> CodeOperation.EXPLAIN

            listOf(
                "gere código",
                "gere codigo",
                "código",
                "codigo",
                "script",
                "programa"
            ).any { text.contains(it) } -> CodeOperation.GENERATE

            else -> CodeOperation.UNKNOWN
        }
    }

    fun needsWebSearch(message: String): Boolean {
        val text = message.lowercase()

        val triggers = listOf(
            "pesquise",
            "pesquisar",
            "pesquisa na web",
            "procure na internet",
            "procura na internet",
            "na internet",
            "web",
            "documentação atual",
            "documentacao atual",
            "documentação oficial",
            "documentacao oficial",
            "última versão",
            "ultima versao",
            "versão atual",
            "versao atual",
            "atualmente"
        )

        return triggers.any { text.contains(it) }
    }

    fun build(
        userRules: List<Rule>,
        history: List<ChatMessage>,
        userMessage: String,
        searchResults: String? = null
    ): String {

        val sb = StringBuilder()

        sb.appendLine(
            "Você é o LuauAI, um assistente especializado em Luau, Roblox " +
            "e desenvolvimento de software."
        )

        sb.appendLine(
            "Responda de forma clara, útil e tecnicamente correta."
        )

        if (userRules.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("REGRAS DO PROJETO:")

            userRules
                .filter { it.enabled }
                .sortedBy { it.priority }
                .forEach { rule ->
                    sb.appendLine("- ${rule.content}")
                }
        }

        if (history.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("HISTÓRICO DA CONVERSA:")

            history.takeLast(20).forEach { message ->
                val role = if (message.isUser) "Usuário" else "Assistente"
                sb.appendLine("$role: ${message.content}")
            }
        }

        if (!searchResults.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("RESULTADOS DA PESQUISA WEB:")
            sb.appendLine(searchResults)
        }

        sb.appendLine()
        sb.appendLine("PEDIDO DO USUÁRIO:")
        sb.appendLine(userMessage)

        sb.appendLine()
        sb.appendLine("RESPOSTA:")

        return sb.toString()
    }
}
