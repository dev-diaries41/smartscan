package com.fpf.smartscan.data.tags

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTagDao {
    @Query("SELECT * FROM user_tags ORDER BY createdAt DESC")
    fun getAllTags(): Flow<List<UserTagEntity>>

    @Query("SELECT * FROM user_tags WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveTags(): Flow<List<UserTagEntity>>

    @Query("SELECT * FROM user_tags WHERE isActive = 1")
    suspend fun getActiveTagsSync(): List<UserTagEntity>

    @Query("SELECT * FROM user_tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): UserTagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: UserTagEntity)

    @Update
    suspend fun updateTag(tag: UserTagEntity)

    @Delete
    suspend fun deleteTag(tag: UserTagEntity)

    @Query("DELETE FROM user_tags WHERE name = :name")
    suspend fun deleteTagByName(name: String)

    @Query("SELECT COUNT(*) FROM user_tags")
    suspend fun getTagCount(): Int
}
