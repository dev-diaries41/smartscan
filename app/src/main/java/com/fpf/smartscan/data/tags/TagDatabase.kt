package com.fpf.smartscan.data.tags

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [UserTagEntity::class, ImageTagEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(TagConverters::class)
abstract class TagDatabase : RoomDatabase() {
    abstract fun userTagDao(): UserTagDao
    abstract fun imageTagDao(): ImageTagDao

    companion object {
        @Volatile
        private var INSTANCE: TagDatabase? = null

        fun getDatabase(context: Context): TagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TagDatabase::class.java,
                    "tag_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
