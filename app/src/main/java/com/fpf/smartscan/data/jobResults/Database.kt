package com.fpf.smartscan.data.jobResults

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [JobResult::class], version = 1, exportSchema = false)
abstract class JobsDatabase : RoomDatabase() {

    abstract fun jobResultDao(): JobResultDao

    companion object {
        @Volatile private var INSTANCE: JobsDatabase? = null

        fun getInstance(context: Context): JobsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JobsDatabase::class.java,
                    "job_result_db"
                ).build().also { INSTANCE = it }
            }
    }
}
