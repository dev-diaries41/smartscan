package com.fpf.smartscan.data.videos

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface VideoEmbeddingDao {

    @Query("SELECT * FROM video_embeddings ORDER BY date DESC")
    fun getAllVideoEmbeddings(): LiveData<List<VideoEmbedding>>

    @Query("SELECT * FROM video_embeddings ORDER BY date DESC")
    suspend fun getAllEmbeddingsSync(): List<VideoEmbedding>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoEmbedding(videoEmbedding: VideoEmbedding)

    @Delete
    suspend fun deleteVideoEmbedding(videoEmbedding: VideoEmbedding)

    @Query("SELECT EXISTS(SELECT 1 FROM video_embeddings WHERE id = :id)")
    suspend fun isVideoIndexed(id: Long): Boolean
}
