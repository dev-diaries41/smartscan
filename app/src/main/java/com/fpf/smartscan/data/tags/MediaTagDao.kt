package com.fpf.smartscan.data.tags

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaTagDao {
    @Query("SELECT * FROM media_tags WHERE mediaId = :mediaId ORDER BY confidence DESC")
    suspend fun getTagsForMedia(mediaId: Long): List<MediaTagEntity>

    @Query("SELECT * FROM media_tags WHERE mediaId = :mediaId ORDER BY confidence DESC")
    fun getTagsForMediaFlow(mediaId: Long): Flow<List<MediaTagEntity>>

    @Query("SELECT DISTINCT mediaId FROM media_tags WHERE tagName = :tagName")
    suspend fun getMediaIdsForTag(tagName: String): List<Long>

    @Query("SELECT DISTINCT mediaId FROM media_tags WHERE tagName IN (:tagNames) GROUP BY mediaId HAVING COUNT(DISTINCT tagName) = :tagCount")
    suspend fun getMediaIdsForTags(tagNames: List<String>, tagCount: Int): List<Long>

    @Query("SELECT DISTINCT mediaId FROM media_tags WHERE tagName IN (:tagNames)")
    suspend fun getMediaIdsWithAnyTag(tagNames: List<String>): List<Long>

    @Query("SELECT tagName, COUNT(*) as mediaCount FROM media_tags GROUP BY tagName")
    suspend fun getTagCounts(): List<TagCount>

    data class TagCount(
        val tagName: String,
        val mediaCount: Int
    )

    @Query("SELECT COUNT(DISTINCT mediaId) FROM media_tags WHERE tagName = :tagName")
    suspend fun getMediaCountForTag(tagName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: MediaTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<MediaTagEntity>)

    @Delete
    suspend fun deleteTag(tag: MediaTagEntity)

    @Query("DELETE FROM media_tags WHERE mediaId = :mediaId")
    suspend fun deleteTagsForMedia(mediaId: Long)

    @Query("DELETE FROM media_tags WHERE mediaId = :mediaId AND tagName = :tagName")
    suspend fun deleteSpecificTag(mediaId: Long, tagName: String)

    @Query("DELETE FROM media_tags WHERE tagName = :tagName")
    suspend fun deleteAllTagsWithName(tagName: String)

    @Query("DELETE FROM media_tags WHERE mediaId IN (:mediaIds)")
    suspend fun deleteTagsForMediaItems(mediaIds: List<Long>)

    @Query("DELETE FROM media_tags WHERE mediaId = :mediaId AND isUserAssigned = 0")
    suspend fun deleteAutoAssignedTagsForMedia(mediaId: Long)
}
