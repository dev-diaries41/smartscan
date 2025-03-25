package com.fpf.smartscan.data.scans

import android.app.Application
import androidx.room.*

// 3. Room Database
@Database(entities = [ScanData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDataDao(): ScanDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(application: Application): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    AppDatabase::class.java,
                    "scan_history_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

