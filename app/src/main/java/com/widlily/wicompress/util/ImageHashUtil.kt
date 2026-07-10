package com.widlily.wicompress.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

object ImageHashUtil {
    private const val TAG = "ImageHashUtil"

    init {
        try {
            System.loadLibrary("native-lib")
            Log.d(TAG, "Native library 'native-lib' loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}")
        }
    }

    // JNI Declarations
    external fun computeHammingDistance(hash1: Long, hash2: Long): Int
    external fun computeAverageHash(pixels: IntArray, width: Int, height: Int): Long

    /**
     * Extracts a keyframe at 1.0 second, scales it to 64x64,
     * and calculates its perceptual average hash using JNI.
     */
    fun getVideoPHash(context: Context, videoPath: String): Long {
        val retriever = MediaMetadataRetriever()
        var bitmap: Bitmap? = null
        try {
            if (videoPath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(videoPath))
            } else {
                retriever.setDataSource(videoPath)
            }
            
            // Extract frame at 1 second (1000000 microseconds)
            bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime // fallback
                
            if (bitmap != null) {
                // Resize to 64x64 to compress pixel array and reduce JNI overhead
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
                val width = scaledBitmap.width
                val height = scaledBitmap.height
                val pixels = IntArray(width * height)
                
                scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                
                // Invoke C++ JNI computation
                val pHash = computeAverageHash(pixels, width, height)
                scaledBitmap.recycle()
                return pHash
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating video pHash: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignored
            }
            bitmap?.recycle()
        }
        return 0L
    }

    /**
     * Helper to verify if two hashes are perceptual duplicates
     * Threshold is typical Hamming distance < 10 (out of 64 bits)
     */
    fun isDuplicate(hash1: Long, hash2: Long, threshold: Int = 8): Boolean {
        if (hash1 == 0L || hash2 == 0L) return false
        val distance = computeHammingDistance(hash1, hash2)
        return distance <= threshold
    }
}
