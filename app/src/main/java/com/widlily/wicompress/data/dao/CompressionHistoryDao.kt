package com.widlily.wicompress.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.widlily.wicompress.data.entity.CompressionHistory
import com.widlily.wicompress.data.entity.SystemStats
import kotlinx.coroutines.flow.Flow

@Dao
interface CompressionHistoryDao {

    @Query("SELECT * FROM compression_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<CompressionHistory>>

    @Query("SELECT * FROM compression_history WHERE success = 1 ORDER BY timestamp DESC")
    suspend fun getAllSuccessfulHistory(): List<CompressionHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CompressionHistory): Long

    @Query("DELETE FROM compression_history")
    suspend fun clearHistory()

    // Aggregations from History table
    @Query("SELECT COUNT(*) FROM compression_history WHERE success = 1")
    fun getSuccessfulCountFlow(): Flow<Int>

    @Query("SELECT SUM(originalSize - compressedSize) FROM compression_history WHERE success = 1")
    fun getTotalSavedBytesFlow(): Flow<Long?>

    // System Stats operations (single record)
    @Query("SELECT * FROM system_stats WHERE id = 0")
    fun getSystemStatsFlow(): Flow<SystemStats?>

    @Query("SELECT * FROM system_stats WHERE id = 0")
    suspend fun getSystemStats(): SystemStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystemStats(stats: SystemStats)
}
