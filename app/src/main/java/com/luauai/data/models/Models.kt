package com.luauai.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Projeto ──────────────────────────────────────────────────────────────────
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ── Arquivo Luau dentro de um projeto ────────────────────────────────────────
@Entity(
    tableName = "project_files",
    foreignKeys = [ForeignKey(
        entity        = Project::class,
        parentColumns = ["id"],
        childColumns  = ["projectId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class ProjectFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,          // ex: "Main.server.lua"
    val content: String = "",
    val fileType: String = "lua", // lua, luau, txt
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ── Regra definida pelo usuário ───────────────────────────────────────────────
@Entity(
    tableName = "rules",
    foreignKeys = [ForeignKey(
        entity        = Project::class,
        parentColumns = ["id"],
        childColumns  = ["projectId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,       // 0 = regra global
    val content: String,
    val priority: Int = 0,     // menor = maior prioridade
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Mensagem do chat ──────────────────────────────────────────────────────────
@Entity(
    tableName = "chat_messages",
    foreignKeys = [ForeignKey(
        entity        = Project::class,
        parentColumns = ["id"],
        childColumns  = ["projectId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val content: String,
    val isUser: Boolean,
    val searchResults: String? = null,  // JSON de SearchResult[]
    val timestamp: Long = System.currentTimeMillis()
)

// ── Resultado de pesquisa web ─────────────────────────────────────────────────
data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val source: String,
    val date: String
)
