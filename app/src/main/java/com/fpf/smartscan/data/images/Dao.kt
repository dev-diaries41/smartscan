package com.fpf.smartscan.data.images

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ImageEmbeddingDao {

    @Query("SELECT * FROM image_embeddings ORDER BY date DESC")
    fun getAllImageEmbeddings(): LiveData<List<ImageEmbedding>>

    @Query("SELECT * FROM image_embeddings ORDER BY date DESC")
    suspend fun getAllEmbeddingsSync(): List<ImageEmbedding>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageEmbedding(imageEmbedding: ImageEmbedding)

    @Query("DELETE FROM image_embeddings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM image_embeddings WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM image_embeddings")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT 1 FROM image_embeddings)")
    fun hasAnyImageEmbedding(): LiveData<Boolean>
}
