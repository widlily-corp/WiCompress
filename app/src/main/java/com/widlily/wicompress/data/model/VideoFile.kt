package com.widlily.wicompress.data.model

import android.net.Uri

data class VideoFile(
    val id: Long,
    val uri: Uri,
    val path: String,
    val displayName: String,
    val size: Long, // Size in bytes
    val duration: Long, // Duration in ms
    val dateAdded: Long,
    var pHash: Long = 0L // Populated during deep search phase 2
) {
    val sizeMb: Float
        get() = size.toFloat() / (1024f * 1024f)
        
    val durationText: String
        get() {
            val totalSecs = duration / 1000
            val hours = totalSecs / 3600
            val minutes = (totalSecs % 3600) / 60
            val seconds = totalSecs % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}
