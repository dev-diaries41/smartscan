package com.fpf.smartscan.data.prototypes

import android.app.Application
import androidx.room.*


// 4. Room Database
@Database(entities = [PrototypeEmbeddingEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PrototypeEmbeddingDatabase : RoomDatabase() {
    abstract fun prototypeEmbeddingDao(): PrototypeEmbeddingDao

    companion object {
        @Volatile
        private var INSTANCE: PrototypeEmbeddingDatabase? = null

        fun getDatabase(application: Application): PrototypeEmbeddingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    application,
                    PrototypeEmbeddingDatabase::class.java,
                    "prototype_embedding_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

