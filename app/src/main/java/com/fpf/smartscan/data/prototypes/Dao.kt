package com.fpf.smartscan.data.prototypes

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PrototypeEmbeddingDao {

    @Query("SELECT * FROM prototype_embeddings ORDER BY date DESC")
    fun getAllEmbeddings(): LiveData<List<PrototypeEmbedding>>

    @Query("SELECT * FROM prototype_embeddings ORDER BY date DESC")
    suspend fun getAllEmbeddingsSync(): List<PrototypeEmbedding>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrototype(prototype: PrototypeEmbedding)

    @Delete
    suspend fun deletePrototype(prototype: PrototypeEmbedding)
}

