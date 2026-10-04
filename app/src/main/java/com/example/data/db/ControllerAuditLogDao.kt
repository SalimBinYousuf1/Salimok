package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ControllerAuditLogDao {
    @Query("SELECT * FROM controller_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<ControllerAuditLogEntity>>

    @Insert
    suspend fun insertLog(log: ControllerAuditLogEntity)

    @Query("DELETE FROM controller_audit_logs")
    suspend fun clearAll()
}
