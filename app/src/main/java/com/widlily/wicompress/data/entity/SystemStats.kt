package com.widlily.wicompress.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_stats")
data class SystemStats(
    @PrimaryKey val id: Int = 0,
    val totalSpaceSavedBytes: Long = 0,
    val totalCompressedCount: Int = 0,
    val averageRatio: Float = 0f // Average space savings percentage, e.g., 44.5f
)
