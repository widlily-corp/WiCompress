package com.widlily.wicompress

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.widlily.wicompress.data.AppDatabase

class WiCompressApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Database eagerly to ensure Room is ready
        database = AppDatabase.getDatabase(this)
        
        // Create foreground notification channel
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.compression_notification_channel)
            val descriptionText = getString(R.string.compression_notification_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "wicompress_background_channel"
        
        private lateinit var instance: WiCompressApp
        lateinit var database: AppDatabase
            private set

        fun getContext(): Context {
            return instance.applicationContext
        }
    }
}
