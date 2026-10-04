package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedHostDao {
    @Query("SELECT * FROM trusted_hosts ORDER BY lastSeen DESC")
    fun getAllHostsFlow(): Flow<List<TrustedHostEntity>>

    @Query("SELECT * FROM trusted_hosts WHERE isPrimary = 1 LIMIT 1")
    fun getPrimaryHostFlow(): Flow<TrustedHostEntity?>

    @Query("SELECT * FROM trusted_hosts WHERE hostId = :hostId LIMIT 1")
    suspend fun getHostById(hostId: String): TrustedHostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHost(host: TrustedHostEntity)

    @Update
    suspend fun updateHost(host: TrustedHostEntity)

    @Query("UPDATE trusted_hosts SET lastSeen = :timestamp WHERE hostId = :hostId")
    suspend fun updateLastSeen(hostId: String, timestamp: Long)

    @Query("""
        UPDATE trusted_hosts SET 
            cachedBatteryPercent = :battery,
            cachedBatteryCharging = :isCharging,
            cachedNetworkType = :networkType,
            cachedDeviceModel = :model,
            cachedAndroidVersion = :androidVer,
            cachedResolution = :resolution,
            lastSeen = :timestamp
        WHERE hostId = :hostId
    """)
    suspend fun updateCachedTelemetry(
        hostId: String,
        battery: Int,
        isCharging: Boolean,
        networkType: String,
        model: String,
        androidVer: String,
        resolution: String,
        timestamp: Long
    )

    @Query("DELETE FROM trusted_hosts WHERE hostId = :hostId")
    suspend fun deleteHost(hostId: String)

    @Query("SELECT COUNT(*) FROM trusted_hosts")
    suspend fun getHostCount(): Int
}
