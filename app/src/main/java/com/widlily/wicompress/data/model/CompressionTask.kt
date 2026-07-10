package com.widlily.wicompress.data.model

import java.util.UUID

data class CompressionTask(
    val id: String = UUID.randomUUID().toString(),
    val inputPath: String,
    val outputPath: String,
    val type: String, // "Quick", "Platform", "Custom Size"
    val originalSize: Long,
    val targetBitrate: Float = 2.0f,
    val useH265: Boolean = false,
    val autoDeleteOriginal: Boolean = false,
    val resolutionWidth: Int? = null,
    val resolutionHeight: Int? = null,
    val displayName: String = inputPath.substringAfterLast("/")
)
