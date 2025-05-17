package com.fpf.smartscan.data.moveHistory

import android.app.Application
import androidx.room.*

// 3. Room Database
@Database(entities = [MoveHistory::class], version = 1, exportSchema = false)
abstract class MoveHistoryDatabase : RoomDatabase() {
    abstract fun moveHistoryDao(): MoveHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MoveHistoryDatabase? = null

        fun getDatabase(application: Application): MoveHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    MoveHistoryDatabase::class.java,
                    "move_history_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

