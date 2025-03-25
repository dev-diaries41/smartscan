package com.fpf.smartscan.data.images

import android.app.Application
import androidx.room.*

@Database(entities = [ImageEmbedding::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ImageEmbeddingDatabase : RoomDatabase() {
    abstract fun imageEmbeddingDao(): ImageEmbeddingDao

    companion object {
        @Volatile
        private var INSTANCE: ImageEmbeddingDatabase? = null

        fun getDatabase(application: Application): ImageEmbeddingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    ImageEmbeddingDatabase::class.java,
                    "image_embedding_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


