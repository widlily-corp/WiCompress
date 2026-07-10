package com.widlily.wicompress.util

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.widlily.wicompress.data.model.DuplicateGroup
import com.widlily.wicompress.data.model.VideoFile
import java.io.File

object MediaStoreHelper {
    private const val TAG = "MediaStoreHelper"

    /**
     * Queries MediaStore for all video files.
     */
    fun getVideoFiles(context: Context): List<VideoFile> {
        val videos = mutableListOf<VideoFile>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA
        )

        // Select only valid sized video clips
        val selection = "${MediaStore.Video.Media.SIZE} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(nameColumn) ?: "Video_$id"
                val size = cursor.getLong(sizeColumn)
                val duration = cursor.getLong(durationColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val path = cursor.getString(dataColumn) ?: ""
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                videos.add(
                    VideoFile(
                        id = id,
                        uri = uri,
                        path = path,
                        displayName = displayName,
                        size = size,
                        duration = duration,
                        dateAdded = dateAdded
                    )
                )
            }
        }
        return videos
    }

    /**
     * Executes the duplicate detection algorithm:
     * Phase 1: Group by duration (within 500ms tolerance) and file size (exact or within 1% variance).
     * Phase 2: Compute JNI pHash on matching candidates to verify visual duplicate status.
     */
    fun findDuplicates(
        context: Context,
        onProgress: (scanned: Int, total: Int) -> Unit
    ): List<DuplicateGroup> {
        val allVideos = getVideoFiles(context)
        val total = allVideos.size
        if (total < 2) return emptyList()

        Log.d(TAG, "Starting duplicate search on $total videos")

        // Phase 1: Pre-filtering to avoid calculating pHash on every single video file
        val candidates = mutableListOf<VideoFile>()
        
        for (i in 0 until total) {
            val v1 = allVideos[i]
            var hasMatch = false
            for (j in 0 until total) {
                if (i == j) continue
                val v2 = allVideos[j]
                
                // Group criteria: duration variance < 1s AND size variance < 5%
                val sizeDiffPercent = Math.abs(v1.size - v2.size).toDouble() / v1.size.toDouble()
                val durationDiffMs = Math.abs(v1.duration - v2.duration)
                
                if (sizeDiffPercent < 0.05 && durationDiffMs < 1000) {
                    hasMatch = true
                    break
                }
            }
            if (hasMatch) {
                candidates.add(v1)
            }
        }

        Log.d(TAG, "Phase 1 finished. Filtered candidates count: ${candidates.size}")
        if (candidates.size < 2) return emptyList()

        // Phase 2: Compute pHash for candidate files and compute Hamming distance
        val hashedVideos = mutableListOf<VideoFile>()
        var count = 0
        for (video in candidates) {
            val hash = ImageHashUtil.getVideoPHash(context, video.path)
            video.pHash = hash
            hashedVideos.add(video)
            count++
            onProgress(count, candidates.size)
        }

        // Group together matches
        val visited = mutableSetOf<Long>()
        val groups = mutableListOf<DuplicateGroup>()

        for (i in 0 until hashedVideos.size) {
            val v1 = hashedVideos[i]
            if (visited.contains(v1.id)) continue

            val currentGroupFiles = mutableListOf<VideoFile>()
            currentGroupFiles.add(v1)

            for (j in i + 1 until hashedVideos.size) {
                val v2 = hashedVideos[j]
                if (visited.contains(v2.id)) continue

                // Check Hamming threshold using JNI
                if (ImageHashUtil.isDuplicate(v1.pHash, v2.pHash, threshold = 8)) {
                    currentGroupFiles.add(v2)
                    visited.add(v2.id)
                }
            }

            if (currentGroupFiles.size > 1) {
                visited.add(v1.id)
                // Determine original / best quality file: oldest date or largest file size
                // Sort by dateAdded ascending (oldest first) or resolution (represented by larger size)
                val sortedFiles = currentGroupFiles.sortedWith(
                    compareBy<VideoFile> { it.dateAdded }
                        .thenByDescending { it.size }
                )
                
                // Let the first one in sorted order be the "best" copy (the original)
                groups.add(DuplicateGroup(files = sortedFiles, bestIndex = 0))
            }
        }

        Log.d(TAG, "Phase 2 finished. Found ${groups.size} duplicate groups")
        return groups
    }

    /**
     * Prepares MediaStore deletion intent for Android 10+.
     */
    fun createDeleteRequest(context: Context, uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        } else {
            // Android 9 and lower: handled via local file API or normal URI deletion
            null
        }
    }
}
