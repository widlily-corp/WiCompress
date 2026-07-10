package com.widlily.wicompress.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.widlily.wicompress.MainActivity
import com.widlily.wicompress.R
import com.widlily.wicompress.WiCompressApp
import com.widlily.wicompress.data.entity.CompressionHistory
import com.widlily.wicompress.data.entity.SystemStats
import com.widlily.wicompress.data.model.CompressionTask
import com.widlily.wicompress.util.FFmpegManager
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompressionService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var activeSessionId: Long? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): CompressionService = this@CompressionService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CompressionService Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action: $action")
        
        when (action) {
            ACTION_ADD_TASK -> {
                val inputPath = intent.getStringExtra(EXTRA_INPUT_PATH) ?: ""
                val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH) ?: ""
                val type = intent.getStringExtra(EXTRA_TYPE) ?: "Quick"
                val size = intent.getLongExtra(EXTRA_SIZE, 0L)
                val bitrate = intent.getFloatExtra(EXTRA_BITRATE, 2.0f)
                val useH265 = intent.getBooleanExtra(EXTRA_USE_H265, false)
                val autoDelete = intent.getBooleanExtra(EXTRA_AUTO_DELETE, false)
                val width = intent.getIntExtra(EXTRA_WIDTH, -1).let { if (it == -1) null else it }
                val height = intent.getIntExtra(EXTRA_HEIGHT, -1).let { if (it == -1) null else it }

                if (inputPath.isNotEmpty() && outputPath.isNotEmpty()) {
                    val task = CompressionTask(
                        inputPath = inputPath,
                        outputPath = outputPath,
                        type = type,
                        originalSize = size,
                        targetBitrate = bitrate,
                        useH265 = useH265,
                        autoDeleteOriginal = autoDelete,
                        resolutionWidth = width,
                        resolutionHeight = height
                    )
                    addTask(task)
                }
            }
            ACTION_PAUSE -> pauseQueue()
            ACTION_CANCEL_CURRENT -> cancelCurrentTask()
            ACTION_STOP_SERVICE -> stopServiceInternal()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "CompressionService Destroyed")
    }

    private fun startForegroundServiceCompat() {
        val notification = createNotification("Initializing compression...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        _isProcessing.value = true
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 
                0, 
                notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, WiCompressApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.compression_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }

    // Task Queue Operations
    private fun addTask(task: CompressionTask) {
        val list = _queueList.value.toMutableList()
        list.add(task)
        _queueList.value = list
        Log.d(TAG, "Task added: ${task.displayName}. Total in queue: ${list.size}")

        if (!_isProcessing.value) {
            startForegroundServiceCompat()
            processNext()
        }
    }

    private fun processNext() {
        if (_queueList.value.isEmpty()) {
            stopServiceInternal()
            return
        }

        val task = _queueList.value.first()
        _currentTask.value = task
        _progress.value = 0f
        _speed.value = 0f
        _etaSeconds.value = 0

        updateNotification("Processing: ${task.displayName}")

        serviceScope.launch(Dispatchers.IO) {
            val fileInput = File(task.inputPath)
            if (!fileInput.exists()) {
                Log.e(TAG, "Input file does not exist: ${task.inputPath}")
                onTaskComplete(success = false, outputPath = null)
                return@launch
            }

            val startTime = System.currentTimeMillis()
            
            // Execute video processing with Kirin accelerated codecs
            val sessionId = FFmpegManager.compressVideo(
                context = this@CompressionService,
                inputPath = task.inputPath,
                outputPath = task.outputPath,
                targetBitrateMbps = task.targetBitrate,
                useH265 = task.useH265,
                resolutionWidth = task.resolutionWidth,
                resolutionHeight = task.resolutionHeight,
                listener = object : FFmpegManager.ProgressListener {
                    override fun onProgress(percentage: Float, speed: Float, etaSeconds: Int) {
                        _progress.value = percentage
                        _speed.value = speed
                        _etaSeconds.value = etaSeconds
                        updateNotification("Processing: ${task.displayName} (${percentage.toInt()}%)")
                    }

                    override fun onComplete(success: Boolean, outputPath: String?) {
                        serviceScope.launch(Dispatchers.IO) {
                            val duration = System.currentTimeMillis() - startTime
                            saveToDatabase(task, success, outputPath, duration)
                            onTaskComplete(success, outputPath)
                        }
                    }
                }
            )
            activeSessionId = sessionId
        }
    }

    private suspend fun saveToDatabase(
        task: CompressionTask,
        success: Boolean,
        outputPath: String?,
        processingDurationMs: Long
    ) {
        val input = File(task.inputPath)
        val output = if (success && outputPath != null) File(outputPath) else null
        
        val record = CompressionHistory(
            fileName = task.displayName,
            originalPath = task.inputPath,
            compressedPath = outputPath ?: "",
            originalSize = input.length(),
            compressedSize = output?.length() ?: 0L,
            compressionType = task.type,
            duration = processingDurationMs,
            success = success
        )

        // Insert log in Room DB
        val dao = WiCompressApp.database.compressionHistoryDao()
        dao.insertHistory(record)

        if (success && output != null) {
            val savedBytes = (input.length() - output.length()).coerceAtLeast(0L)
            
            // Update session metrics
            _sessionSavedBytes.value += savedBytes
            _completedCount.value += 1

            // Update persistent System Statistics
            val stats = dao.getSystemStats() ?: SystemStats()
            val newCount = stats.totalCompressedCount + 1
            val newSaved = stats.totalSpaceSavedBytes + savedBytes
            
            // Calculate running average compression ratio
            val newRatio = ((stats.averageRatio * stats.totalCompressedCount) + record.ratioPercent) / newCount
            
            dao.insertSystemStats(
                SystemStats(
                    totalSpaceSavedBytes = newSaved,
                    totalCompressedCount = newCount,
                    averageRatio = newRatio
                )
            )

            // Handle "Auto-delete original"
            if (task.autoDeleteOriginal) {
                // Verify new file integrity: size > 0 and duration matches (within tolerance)
                val newDuration = FFmpegManager.getVideoDuration(this@CompressionService, output.absolutePath)
                val originalDuration = FFmpegManager.getVideoDuration(this@CompressionService, input.absolutePath)
                
                if (output.length() > 0 && Math.abs(newDuration - originalDuration) < 1000) {
                    try {
                        // Delete original file
                        val deleted = input.delete()
                        if (deleted) {
                            Log.i(TAG, "Successfully auto-deleted original file: ${task.inputPath}")
                        } else {
                            // On Android 11+ this might fail. We can delete it using contentResolver or standard file API
                            contentResolver.delete(
                                Uri.parse(task.inputPath), // If it's content:// Uri
                                null, 
                                null
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete original: ${e.message}")
                    }
                } else {
                    Log.e(TAG, "Integrity check failed. Auto-deletion aborted.")
                }
            }
        }
    }

    private fun onTaskComplete(success: Boolean, outputPath: String?) {
        activeSessionId = null
        val currentQueue = _queueList.value.toMutableList()
        if (currentQueue.isNotEmpty()) {
            currentQueue.removeAt(0)
            _queueList.value = currentQueue
        }

        // Run next task
        processNext()
    }

    private fun pauseQueue() {
        // Pause cancels current active session and stops processor
        cancelCurrentTask()
        _isProcessing.value = false
        updateNotification("Queue paused")
    }

    private fun cancelCurrentTask() {
        activeSessionId?.let {
            FFmpegManager.cancelSession(it)
            activeSessionId = null
        }
    }

    private fun stopServiceInternal() {
        cancelCurrentTask()
        _isProcessing.value = false
        _currentTask.value = null
        _queueList.value = emptyList()
        stopForeground(true)
        stopSelf()
    }

    companion object {
        private const val TAG = "CompressionService"
        private const val NOTIFICATION_ID = 2026
        
        const val ACTION_ADD_TASK = "com.widlily.wicompress.ACTION_ADD_TASK"
        const val ACTION_PAUSE = "com.widlily.wicompress.ACTION_PAUSE"
        const val ACTION_CANCEL_CURRENT = "com.widlily.wicompress.ACTION_CANCEL_CURRENT"
        const val ACTION_STOP_SERVICE = "com.widlily.wicompress.ACTION_STOP_SERVICE"

        const val EXTRA_INPUT_PATH = "EXTRA_INPUT_PATH"
        const val EXTRA_OUTPUT_PATH = "EXTRA_OUTPUT_PATH"
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_SIZE = "EXTRA_SIZE"
        const val EXTRA_BITRATE = "EXTRA_BITRATE"
        const val EXTRA_USE_H265 = "EXTRA_USE_H265"
        const val EXTRA_AUTO_DELETE = "EXTRA_AUTO_DELETE"
        const val EXTRA_WIDTH = "EXTRA_WIDTH"
        const val EXTRA_HEIGHT = "EXTRA_HEIGHT"

        // State flows accessible from the Compose UI layer
        private val _currentTask = MutableStateFlow<CompressionTask?>(null)
        val currentTask: StateFlow<CompressionTask?> = _currentTask.asStateFlow()

        private val _queueList = MutableStateFlow<List<CompressionTask>>(emptyList())
        val queueList: StateFlow<List<CompressionTask>> = _queueList.asStateFlow()

        private val _progress = MutableStateFlow(0f)
        val progress: StateFlow<Float> = _progress.asStateFlow()

        private val _speed = MutableStateFlow(0f)
        val speed: StateFlow<Float> = _speed.asStateFlow()

        private val _etaSeconds = MutableStateFlow(0)
        val etaSeconds: StateFlow<Int> = _etaSeconds.asStateFlow()

        private val _isProcessing = MutableStateFlow(false)
        val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

        private val _completedCount = MutableStateFlow(0)
        val completedCount: StateFlow<Int> = _completedCount.asStateFlow()

        private val _sessionSavedBytes = MutableStateFlow(0L)
        val sessionSavedBytes: StateFlow<Long> = _sessionSavedBytes.asStateFlow()
    }
}
