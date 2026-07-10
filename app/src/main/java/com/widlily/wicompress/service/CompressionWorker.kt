package com.widlily.wicompress.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CompressionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val inputPath = inputData.getString(KEY_INPUT_PATH) ?: return Result.failure()
        val outputPath = inputData.getString(KEY_OUTPUT_PATH) ?: return Result.failure()
        val type = inputData.getString(KEY_TYPE) ?: "Quick"
        val size = inputData.getLong(KEY_SIZE, 0L)
        val bitrate = inputData.getFloat(KEY_BITRATE, 2.0f)
        val useH265 = inputData.getBoolean(KEY_USE_H265, false)
        val autoDelete = inputData.getBoolean(KEY_AUTO_DELETE, false)

        Log.d(TAG, "WorkManager worker starting compression service for $inputPath")

        try {
            // Trigger the Foreground service to handle the compression securely
            val intent = Intent(applicationContext, CompressionService::class.java).apply {
                action = CompressionService.ACTION_ADD_TASK
                putExtra(CompressionService.EXTRA_INPUT_PATH, inputPath)
                putExtra(CompressionService.EXTRA_OUTPUT_PATH, outputPath)
                putExtra(CompressionService.EXTRA_TYPE, type)
                putExtra(CompressionService.EXTRA_SIZE, size)
                putExtra(CompressionService.EXTRA_BITRATE, bitrate)
                putExtra(CompressionService.EXTRA_USE_H265, useH265)
                putExtra(CompressionService.EXTRA_AUTO_DELETE, autoDelete)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting compression service from Worker: ${e.message}")
            return Result.failure()
        }
    }

    companion object {
        private const val TAG = "CompressionWorker"
        
        const val KEY_INPUT_PATH = "KEY_INPUT_PATH"
        const val KEY_OUTPUT_PATH = "KEY_OUTPUT_PATH"
        const val KEY_TYPE = "KEY_TYPE"
        const val KEY_SIZE = "KEY_SIZE"
        const val KEY_BITRATE = "KEY_BITRATE"
        const val KEY_USE_H265 = "KEY_USE_H265"
        const val KEY_AUTO_DELETE = "KEY_AUTO_DELETE"
    }
}
