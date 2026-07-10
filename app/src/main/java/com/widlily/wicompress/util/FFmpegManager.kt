package com.widlily.wicompress.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.Session
import java.io.File

object FFmpegManager {
    private const val TAG = "FFmpegManager"

    interface ProgressListener {
        fun onProgress(percentage: Float, speed: Float, etaSeconds: Int)
        fun onComplete(success: Boolean, outputPath: String?)
    }

    /**
     * Obtains the video duration in milliseconds using MediaMetadataRetriever
     */
    fun getVideoDuration(context: Context, videoPath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            if (videoPath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(videoPath))
            } else {
                retriever.setDataSource(videoPath)
            }
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video duration: ${e.message}")
            0L
        } finally {
            retriever.release()
        }
    }

    /**
     * Triggers hardware accelerated compression using MediaCodec encoders.
     */
    fun compressVideo(
        context: Context,
        inputPath: String,
        outputPath: String,
        targetBitrateMbps: Float = 2.0f,
        useH265: Boolean = false,
        resolutionWidth: Int? = null,
        resolutionHeight: Int? = null,
        listener: ProgressListener
    ): Long {
        val durationMs = getVideoDuration(context, inputPath)
        
        // Choose encoder: h264_mediacodec or hevc_mediacodec for Kirin acceleration
        val encoder = if (useH265) "hevc_mediacodec" else "h264_mediacodec"
        val bitrate = "${(targetBitrateMbps * 1000).toInt()}k"
        
        val scaleFilter = if (resolutionWidth != null && resolutionHeight != null) {
            "-vf scale=$resolutionWidth:$resolutionHeight"
        } else {
            ""
        }

        // Build native hardware-accelerated command
        // Note: -y auto-overwrites outputs, -c:v specifies Kirin MediaCodec NDK encoder
        val command = if (scaleFilter.isNotEmpty()) {
            "-y -i \"$inputPath\" $scaleFilter -c:v $encoder -b:v $bitrate -c:a aac -b:a 128k \"$outputPath\""
        } else {
            "-y -i \"$inputPath\" -c:v $encoder -b:v $bitrate -c:a aac -b:a 128k \"$outputPath\""
        }

        Log.d(TAG, "Executing FFmpeg command: $command")

        val session = FFmpegKit.executeAsync(
            command,
            { completedSession ->
                val returnCode = completedSession.returnCode
                val success = returnCode.isValueSuccess
                Log.d(TAG, "FFmpeg task finished with code: $returnCode")
                listener.onComplete(success, if (success) outputPath else null)
            },
            { logMessage ->
                // Optionally forward internal logs
                Log.v(TAG, "[FFmpeg Log] ${logMessage.message}")
            },
            { statistics ->
                if (durationMs > 0) {
                    val timeInMillis = statistics.time
                    val progress = (timeInMillis.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * 100f
                    val speed = statistics.speed.toFloat()
                    
                    val remainingDurationMs = durationMs - timeInMillis
                    val etaSeconds = if (speed > 0) {
                        (remainingDurationMs / (1000 * speed)).toInt().coerceAtLeast(0)
                    } else {
                        0
                    }
                    listener.onProgress(progress, speed, etaSeconds)
                }
            }
        )
        return session.sessionId
    }

    /**
     * Runs custom FFmpeg actions (Trim, Gif conversion, speed scaling, audio extract).
     */
    fun runToolCommand(
        context: Context,
        inputPath: String,
        outputPath: String,
        toolType: String, // "trim", "audio", "gif", "speed"
        params: Map<String, Any>,
        listener: ProgressListener
    ): Long {
        val durationMs = getVideoDuration(context, inputPath)
        
        val command = when (toolType.lowercase()) {
            "trim" -> {
                val start = params["start"] ?: "0"
                val end = params["end"] ?: "10"
                "-y -ss $start -to $end -i \"$inputPath\" -c:v h264_mediacodec -c:a aac \"$outputPath\""
            }
            "audio" -> {
                // Extract audio to MP3 format
                "-y -i \"$inputPath\" -vn -c:a libmp3lame -q:a 2 \"$outputPath\""
            }
            "gif" -> {
                // Convert to high-quality palette-based GIF
                "-y -i \"$inputPath\" -vf \"scale=320:-1,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" \"$outputPath\""
            }
            "speed" -> {
                val speedMultiplier = params["speed"] as? Float ?: 2.0f
                val videoFilter = "setpts=${1 / speedMultiplier}*PTS"
                val audioFilter = "atempo=$speedMultiplier"
                "-y -i \"$inputPath\" -filter_complex \"[0:v]$videoFilter[v];[0:a]$audioFilter[a]\" -map \"[v]\" -map \"[a]\" -c:v h264_mediacodec -c:a aac \"$outputPath\""
            }
            else -> {
                "-y -i \"$inputPath\" -c:v copy -c:a copy \"$outputPath\""
            }
        }

        Log.d(TAG, "Executing FFmpeg Tool command ($toolType): $command")

        val session = FFmpegKit.executeAsync(
            command,
            { completedSession ->
                val success = completedSession.returnCode.isValueSuccess
                listener.onComplete(success, if (success) outputPath else null)
            },
            {},
            { statistics ->
                if (durationMs > 0) {
                    val progress = (statistics.time.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * 100f
                    listener.onProgress(progress, statistics.speed.toFloat(), 0)
                }
            }
        )
        return session.sessionId
    }

    /**
     * Cancels active FFmpeg session by sessionId.
     */
    fun cancelSession(sessionId: Long) {
        FFmpegKit.cancel(sessionId)
    }
}
