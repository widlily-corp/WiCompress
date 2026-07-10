package com.widlily.wicompress.data.model

import java.util.UUID

data class DuplicateGroup(
    val id: String = UUID.randomUUID().toString(),
    val files: List<VideoFile>,
    val bestIndex: Int = 0 // Index of the file determined as the highest quality/original to keep
) {
    // Helper to auto-select lower quality / duplicate copies for deletion
    fun getDuplicateIndicesToDelete(): List<Int> {
        return files.indices.filter { it != bestIndex }
    }
}
