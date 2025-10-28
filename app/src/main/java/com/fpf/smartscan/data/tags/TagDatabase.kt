package com.fpf.smartscan.data.tags

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserTagEntity::class, MediaTagEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(TagConverters::class)
abstract class TagDatabase : RoomDatabase() {
    abstract fun userTagDao(): UserTagDao
    abstract fun mediaTagDao(): MediaTagDao

    companion object {
        @Volatile
        private var INSTANCE: TagDatabase? = null

        // Migrace z verze 1 na 2: Přejmenování image_tags na media_tags a imageId na mediaId
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Vytvoříme novou tabulku s novým jménem a strukturou
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `media_tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mediaId` INTEGER NOT NULL,
                        `tagName` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `isUserAssigned` INTEGER NOT NULL DEFAULT 0,
                        `assignedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`tagName`) REFERENCES `user_tags`(`name`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // Vytvoříme indexy
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_tags_mediaId` ON `media_tags` (`mediaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_tags_tagName` ON `media_tags` (`tagName`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_media_tags_mediaId_tagName` ON `media_tags` (`mediaId`, `tagName`)")

                // Zkopírujeme data ze staré tabulky do nové (imageId -> mediaId)
                db.execSQL("""
                    INSERT INTO media_tags (id, mediaId, tagName, confidence, isUserAssigned, assignedAt)
                    SELECT id, imageId, tagName, confidence, isUserAssigned, assignedAt
                    FROM image_tags
                """.trimIndent())

                // Smažeme starou tabulku
                db.execSQL("DROP TABLE IF EXISTS image_tags")
            }
        }

        // Migrace z verze 2 na 3: Přidání isExcluded sloupce do user_tags
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Přidat nový sloupec isExcluded s defaultní hodnotou false (0)
                db.execSQL("ALTER TABLE user_tags ADD COLUMN isExcluded INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): TagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TagDatabase::class.java,
                    "tag_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
