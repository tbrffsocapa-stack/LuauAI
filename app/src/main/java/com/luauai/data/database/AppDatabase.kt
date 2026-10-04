package com.luauai.data.database

import android.content.Context
import androidx.room.*
import com.luauai.data.models.*

@Database(
    entities = [Project::class, ProjectFile::class, Rule::class, ChatMessage::class],
    version  = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileDao(): ProjectFileDao
    abstract fun rulesDao(): RulesDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "luauai.db"
                ).build().also { INSTANCE = it }
            }
    }
}

// ── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY name ASC")
    fun getFiles(projectId: Long): kotlinx.coroutines.flow.Flow<List<ProjectFile>>

    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getFileById(id: Long): ProjectFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFile): Long

    @Update
    suspend fun updateFile(file: ProjectFile)

    @Delete
    suspend fun deleteFile(file: ProjectFile)
}

@Dao
interface ProjectFileDao {
    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY name ASC")
    fun getByProject(projectId: Long): kotlinx.coroutines.flow.Flow<List<ProjectFile>>

    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getById(id: Long): ProjectFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: ProjectFile): Long

    @Update
    suspend fun update(file: ProjectFile)

    @Delete
    suspend fun delete(file: ProjectFile)
}

@Dao
interface RulesDao {
    @Query("SELECT * FROM rules WHERE projectId = :projectId AND enabled = 1 ORDER BY priority ASC")
    fun getForProject(projectId: Long): kotlinx.coroutines.flow.Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE projectId = 0 AND enabled = 1 ORDER BY priority ASC")
    fun getGlobal(): kotlinx.coroutines.flow.Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE (projectId = :projectId OR projectId = 0) AND enabled = 1 ORDER BY priority ASC")
    suspend fun getActiveForProject(projectId: Long): List<Rule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: Rule): Long

    @Update
    suspend fun update(rule: Rule)

    @Delete
    suspend fun delete(rule: Rule)

    @Query("DELETE FROM rules WHERE projectId = :projectId")
    suspend fun deleteAllForProject(projectId: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getHistory(projectId: Long): kotlinx.coroutines.flow.Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    suspend fun getHistorySync(projectId: Long): List<ChatMessage>

    @Insert
    suspend fun insert(msg: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    suspend fun clearHistory(projectId: Long)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun delete(id: Long)
}
