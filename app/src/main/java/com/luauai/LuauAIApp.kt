package com.luauai

import android.app.Application
import com.luauai.data.database.AppDatabase
import com.luauai.data.repository.ProjectRepository
import com.luauai.data.repository.RulesRepository
import com.luauai.data.repository.ChatRepository
import com.luauai.ai.engine.LlamaEngine
import com.luauai.ai.search.WebSearchEngine

class LuauAIApp : Application() {

    // Singletons acessíveis globalmente
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val llamaEngine: LlamaEngine by lazy { LlamaEngine() }
    val webSearchEngine: WebSearchEngine by lazy { WebSearchEngine() }
    val projectRepository: ProjectRepository by lazy { ProjectRepository(database.projectDao()) }
    val rulesRepository: RulesRepository by lazy { RulesRepository(database.rulesDao(), this) }
    val chatRepository: ChatRepository by lazy { ChatRepository(database.chatDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LuauAIApp
            private set
    }
}
