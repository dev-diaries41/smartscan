package com.fpf.smartscan.data.tags

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageTagDao {
    @Query("SELECT * FROM image_tags WHERE imageId = :imageId ORDER BY confidence DESC")
    suspend fun getTagsForImage(imageId: Long): List<ImageTagEntity>

    @Query("SELECT * FROM image_tags WHERE imageId = :imageId ORDER BY confidence DESC")
    fun getTagsForImageFlow(imageId: Long): Flow<List<ImageTagEntity>>

    @Query("SELECT DISTINCT imageId FROM image_tags WHERE tagName = :tagName")
    suspend fun getImageIdsForTag(tagName: String): List<Long>

    @Query("SELECT DISTINCT imageId FROM image_tags WHERE tagName IN (:tagNames) GROUP BY imageId HAVING COUNT(DISTINCT tagName) = :tagCount")
    suspend fun getImageIdsForTags(tagNames: List<String>, tagCount: Int): List<Long>

    @Query("SELECT tagName, COUNT(*) as imageCount FROM image_tags GROUP BY tagName")
    suspend fun getTagCounts(): List<TagCount>

    data class TagCount(
        val tagName: String,
        val imageCount: Int
    )

    @Query("SELECT COUNT(DISTINCT imageId) FROM image_tags WHERE tagName = :tagName")
    suspend fun getImageCountForTag(tagName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: ImageTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<ImageTagEntity>)

    @Delete
    suspend fun deleteTag(tag: ImageTagEntity)

    @Query("DELETE FROM image_tags WHERE imageId = :imageId")
    suspend fun deleteTagsForImage(imageId: Long)

    @Query("DELETE FROM image_tags WHERE imageId = :imageId AND tagName = :tagName")
    suspend fun deleteSpecificTag(imageId: Long, tagName: String)

    @Query("DELETE FROM image_tags WHERE tagName = :tagName")
    suspend fun deleteAllTagsWithName(tagName: String)

    @Query("DELETE FROM image_tags WHERE imageId IN (:imageIds)")
    suspend fun deleteTagsForImages(imageIds: List<Long>)
}
