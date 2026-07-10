package com.widlily.wicompress.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compression_history")
data class CompressionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val originalPath: String,
    val compressedPath: String,
    val originalSize: Long,
    val compressedSize: Long,
    val compressionType: String, // "Quick", "Platform", "Smart Scan", "Batch", "Custom Size"
    val duration: Long, // Process duration in milliseconds
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true
) {
    val savedBytes: Long
        get() = if (success) (originalSize - compressedSize).coerceAtLeast(0L) else 0L

    val ratioPercent: Int
        get() = if (originalSize > 0 && success) {
            val ratio = (compressedSize.toDouble() / originalSize.toDouble()) * 100
            (100 - ratio.toInt()).coerceIn(0, 100)
        } else {
            0
        }
}
