package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.SyncLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<SyncLogEntry>>

    @Insert
    suspend fun insertLog(entry: SyncLogEntry)

    @Query("DELETE FROM sync_logs")
    suspend fun clearLogs()
}
