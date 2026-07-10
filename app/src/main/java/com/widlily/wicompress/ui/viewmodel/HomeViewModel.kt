package com.widlily.wicompress.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.widlily.wicompress.WiCompressApp
import com.widlily.wicompress.data.entity.CompressionHistory
import com.widlily.wicompress.data.entity.SystemStats
import com.widlily.wicompress.data.model.VideoFile
import com.widlily.wicompress.data.repository.SettingsRepository
import com.widlily.wicompress.service.CompressionService
import com.widlily.wicompress.util.MediaStoreHelper
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val db = WiCompressApp.database
    private val settingsRepository = SettingsRepository(context)

    // Flow from Room history and stats
    val systemStats: StateFlow<SystemStats> = db.compressionHistoryDao().getSystemStatsFlow()
        .map { it ?: SystemStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemStats())

    val recentCompressed: StateFlow<List<CompressionHistory>> = db.compressionHistoryDao().getAllHistoryFlow()
        .map { list -> list.take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // MediaStore scanned stats
    private val _totalVideosVolume = MutableStateFlow(0L)
    val totalVideosVolume: StateFlow<Long> = _totalVideosVolume.asStateFlow()

    private val _totalVideosCount = MutableStateFlow(0)
    val totalVideosCount: StateFlow<Int> = _totalVideosCount.asStateFlow()

    // Smart Recommendation banner properties
    private val _smartSuggestionText = MutableStateFlow<String?>(null)
    val smartSuggestionText: StateFlow<String?> = _smartSuggestionText.asStateFlow()

    private val _largeVideos = MutableStateFlow<List<VideoFile>>(emptyList())
    val largeVideos: StateFlow<List<VideoFile>> = _largeVideos.asStateFlow()

    init {
        scanDeviceVideos()
    }

    /**
     * Triggers hardware vibrator feedback using Huawei X-axis haptic simulation (short sharp ticks).
     */
    fun triggerHapticFeedback() {
        if (!settingsRepository.hapticEnabled.value) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    /**
     * Refreshes the local video scan on the device storage.
     */
    fun scanDeviceVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            val videos = MediaStoreHelper.getVideoFiles(context)
            val count = videos.size
            val volume = videos.sumOf { it.size }
            
            _totalVideosCount.value = count
            _totalVideosVolume.value = volume

            // Scan for videos larger than 50MB (52,428,800 bytes)
            val largeFiles = videos.filter { it.size > 50 * 1024 * 1024 }
            _largeVideos.value = largeFiles

            if (largeFiles.isNotEmpty()) {
                val totalLargeSizeGb = largeFiles.sumOf { it.size }.toFloat() / (1024f * 1024f * 1024f)
                val estimatedSavingsGb = totalLargeSizeGb * 0.55f // Estimate 55% average savings
                _smartSuggestionText.value = String.format(
                    "%d large videos found. Compress all to save ~%.1f GB",
                    largeFiles.size,
                    estimatedSavingsGb
                )
            } else {
                _smartSuggestionText.value = null
            }
        }
    }

    /**
     * Launches compression for a single or list of videos.
     */
    fun compressVideo(
        video: VideoFile, 
        type: String = "Quick",
        bitrateMbps: Float = 2.0f,
        useH265: Boolean = false,
        width: Int? = null,
        height: Int? = null
    ) {
        triggerHapticFeedback()
        val defaultOutputDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            settingsRepository.outputDirectory.value
        ).apply { if (!exists()) mkdirs() }

        val outputFileName = "compressed_${System.currentTimeMillis()}_${video.displayName.substringBeforeLast(".")}.mp4"
        val outputFile = File(defaultOutputDir, outputFileName)

        val intent = Intent(context, CompressionService::class.java).apply {
            action = CompressionService.ACTION_ADD_TASK
            putExtra(CompressionService.EXTRA_INPUT_PATH, video.path)
            putExtra(CompressionService.EXTRA_OUTPUT_PATH, outputFile.absolutePath)
            putExtra(CompressionService.EXTRA_TYPE, type)
            putExtra(CompressionService.EXTRA_SIZE, video.size)
            putExtra(CompressionService.EXTRA_BITRATE, bitrateMbps)
            putExtra(CompressionService.EXTRA_USE_H265, useH265)
            putExtra(CompressionService.EXTRA_AUTO_DELETE, settingsRepository.autoDeleteEnabled.value)
            width?.let { putExtra(CompressionService.EXTRA_WIDTH, it) }
            height?.let { putExtra(CompressionService.EXTRA_HEIGHT, it) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * Batch compress all recommended large videos.
     */
    fun compressAllLargeVideos() {
        viewModelScope.launch(Dispatchers.Default) {
            largeVideos.value.forEach { video ->
                compressVideo(video, type = "Smart Scan", bitrateMbps = 1.5f, useH265 = true)
            }
        }
    }
}
