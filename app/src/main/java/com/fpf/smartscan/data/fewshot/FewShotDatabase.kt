package com.fpf.smartscan.data.fewshot

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Few-Shot Learning Database
 *
 * Room database pro ukládání few-shot prototypes a samples.
 *
 * Entities:
 * - FewShotPrototypeEntity: Průměrné embeddingy (prototypes)
 * - FewShotSampleEntity: Jednotlivé sample embeddingy
 *
 * Version 1: Initial release
 * - Tabulka few_shot_prototypes
 * - Tabulka few_shot_samples s foreign key na prototypes (CASCADE delete)
 */
@Database(
    entities = [
        FewShotPrototypeEntity::class,
        FewShotSampleEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(FewShotConverters::class)
abstract class FewShotDatabase : RoomDatabase() {

    abstract fun prototypeDao(): FewShotPrototypeDao
    abstract fun sampleDao(): FewShotSampleDao

    companion object {
        @Volatile
        private var INSTANCE: FewShotDatabase? = null

        private const val DATABASE_NAME = "fewshot_database"

        /**
         * Získá singleton instanci databáze
         *
         * @param context Application context
         * @return FewShotDatabase instance
         */
        fun getDatabase(context: Context): FewShotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FewShotDatabase::class.java,
                    DATABASE_NAME
                )
                // Přidáme migrace zde v budoucnu při změnách schématu
                // .addMigrations(MIGRATION_1_2)
                .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Pro testování - umožňuje vytvořit in-memory databázi
         */
        fun createInMemoryDatabase(context: Context): FewShotDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                FewShotDatabase::class.java
            ).build()
        }
    }
}
