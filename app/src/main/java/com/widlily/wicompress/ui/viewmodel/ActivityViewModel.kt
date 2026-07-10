package com.widlily.wicompress.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.widlily.wicompress.data.model.CompressionTask
import com.widlily.wicompress.data.repository.SettingsRepository
import com.widlily.wicompress.service.CompressionService
import kotlinx.coroutines.flow.StateFlow

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val settingsRepository = SettingsRepository(context)

    // Bridge directly to CompressionService's reactive queues
    val currentTask: StateFlow<CompressionTask?> = CompressionService.currentTask
    val queueList: StateFlow<List<CompressionTask>> = CompressionService.queueList
    val progress: StateFlow<Float> = CompressionService.progress
    val speed: StateFlow<Float> = CompressionService.speed
    val etaSeconds: StateFlow<Int> = CompressionService.etaSeconds
    val isProcessing: StateFlow<Boolean> = CompressionService.isProcessing
    val completedCount: StateFlow<Int> = CompressionService.completedCount
    val sessionSavedBytes: StateFlow<Long> = CompressionService.sessionSavedBytes

    private fun triggerHapticFeedback() {
        if (!settingsRepository.hapticEnabled.value) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    fun pauseQueue() {
        triggerHapticFeedback()
        val intent = Intent(context, CompressionService::class.java).apply {
            action = CompressionService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeQueue() {
        triggerHapticFeedback()
        if (queueList.value.isNotEmpty()) {
            val task = queueList.value.first()
            val intent = Intent(context, CompressionService::class.java).apply {
                action = CompressionService.ACTION_ADD_TASK
                putExtra(CompressionService.EXTRA_INPUT_PATH, task.inputPath)
                putExtra(CompressionService.EXTRA_OUTPUT_PATH, task.outputPath)
                putExtra(CompressionService.EXTRA_TYPE, task.type)
                putExtra(CompressionService.EXTRA_SIZE, task.originalSize)
                putExtra(CompressionService.EXTRA_BITRATE, task.targetBitrate)
                putExtra(CompressionService.EXTRA_USE_H265, task.useH265)
                putExtra(CompressionService.EXTRA_AUTO_DELETE, task.autoDeleteOriginal)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun cancelCurrentTask() {
        triggerHapticFeedback()
        val intent = Intent(context, CompressionService::class.java).apply {
            action = CompressionService.ACTION_CANCEL_CURRENT
        }
        context.startService(intent)
    }

    fun cancelAll() {
        triggerHapticFeedback()
        val intent = Intent(context, CompressionService::class.java).apply {
            action = CompressionService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }
}
