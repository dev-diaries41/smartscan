package com.fpf.smartscan.data.videos

import android.app.Application
import androidx.room.*

@Database(entities = [VideoEmbeddingEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class VideoEmbeddingDatabase : RoomDatabase() {
    abstract fun videoEmbeddingDao(): VideoEmbeddingDao

    companion object {
        @Volatile
        private var INSTANCE: VideoEmbeddingDatabase? = null

        fun getDatabase(application: Application): VideoEmbeddingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    VideoEmbeddingDatabase::class.java,
                    "video_embedding_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


