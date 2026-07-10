package com.widlily.wicompress.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.widlily.wicompress.data.dao.CompressionHistoryDao
import com.widlily.wicompress.data.entity.CompressionHistory
import com.widlily.wicompress.data.entity.SystemStats

@Database(entities = [CompressionHistory::class, SystemStats::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun compressionHistoryDao(): CompressionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wicompress_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
