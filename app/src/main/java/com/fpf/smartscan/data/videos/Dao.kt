package com.fpf.smartscan.data.videos

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface VideoEmbeddingDao {

    @Query("SELECT * FROM video_embeddings ORDER BY date DESC")
    fun getAllVideoEmbeddings(): LiveData<List<VideoEmbeddingEntity>>

    @Query("SELECT * FROM video_embeddings ORDER BY date DESC")
    suspend fun getAllEmbeddingsSync(): List<VideoEmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoEmbedding(videoEmbeddingEntity: VideoEmbeddingEntity)

    @Query("DELETE FROM video_embeddings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM video_embeddings WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM video_embeddings")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT 1 FROM video_embeddings)")
    fun hasAnyVideoEmbeddings(): LiveData<Boolean>
}
