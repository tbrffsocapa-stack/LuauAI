package com.luauai.data.repository

import android.content.Context
import com.luauai.data.database.*
import com.luauai.data.models.*
import kotlinx.coroutines.flow.Flow

// ── ProjectRepository ─────────────────────────────────────────────────────────
class ProjectRepository(private val dao: ProjectDao) {

    fun getAllProjects(): Flow<List<Project>> = dao.getAll()
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun create(name: String, description: String = ""): Long =
        dao.insert(Project(name = name, description = description))
    suspend fun update(project: Project) = dao.update(project.copy(updatedAt = System.currentTimeMillis()))
    suspend fun delete(project: Project) = dao.delete(project)

    fun getFiles(projectId: Long): Flow<List<ProjectFile>> = dao.getFiles(projectId)
    suspend fun getFileById(id: Long) = dao.getFileById(id)
    suspend fun createFile(projectId: Long, name: String, content: String = ""): Long =
        dao.insertFile(ProjectFile(projectId = projectId, name = name, content = content))
    suspend fun updateFile(file: ProjectFile) = dao.updateFile(file.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteFile(file: ProjectFile) = dao.deleteFile(file)
}

// ── RulesRepository ──────────────────────────────────────────────────────────
class RulesRepository(private val dao: RulesDao, private val context: Context) {

    fun getGlobalRules(): Flow<List<Rule>> = dao.getGlobal()
    fun getProjectRules(projectId: Long): Flow<List<Rule>> = dao.getForProject(projectId)
    suspend fun getActiveRules(projectId: Long): List<Rule> = dao.getActiveForProject(projectId)

    suspend fun addRule(projectId: Long, content: String, priority: Int = 0): Long =
        dao.insert(Rule(projectId = projectId, content = content, priority = priority))

    suspend fun updateRule(rule: Rule) = dao.update(rule)
    suspend fun deleteRule(rule: Rule) = dao.delete(rule)
    suspend fun toggleRule(rule: Rule) = dao.update(rule.copy(enabled = !rule.enabled))

    // Inserir regras padrão para um novo projeto
    suspend fun insertDefaults(projectId: Long) {
        val defaults = listOf(
            "Você é especialista em Luau e Roblox.",
            "Responda sempre em português.",
            "Quando modificar um script, mostre o código completo.",
            "Preserve o código original quando modificar apenas uma parte.",
            "Explique o que cada parte do código faz.",
            "Sempre use tipos Luau quando possível.",
            "Aponte possíveis erros ou melhorias ao analisar código."
        )
        defaults.forEachIndexed { index, content ->
            dao.insert(Rule(projectId = projectId, content = content, priority = index))
        }
    }
}

// ── ChatRepository ────────────────────────────────────────────────────────────
class ChatRepository(private val dao: ChatDao) {

    fun getHistory(projectId: Long): Flow<List<ChatMessage>> = dao.getHistory(projectId)
    suspend fun getHistorySync(projectId: Long): List<ChatMessage> = dao.getHistorySync(projectId)
    suspend fun addMessage(msg: ChatMessage): Long = dao.insert(msg)
    suspend fun clearHistory(projectId: Long) = dao.clearHistory(projectId)
    suspend fun deleteMessage(id: Long) = dao.delete(id)
}
