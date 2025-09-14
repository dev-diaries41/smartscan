package com.fpf.smartscan.data.prototypes

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrototypeEmbeddingDao {

    @Query("SELECT * FROM prototype_embeddings ORDER BY date DESC")
    fun getAllEmbeddings(): Flow<List<PrototypeEmbeddingEntity>>

    @Query("SELECT * FROM prototype_embeddings ORDER BY date DESC")
    suspend fun getAllEmbeddingsSync(): List<PrototypeEmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrototype(prototype: PrototypeEmbeddingEntity)

    @Delete
    suspend fun deletePrototype(prototype: PrototypeEmbeddingEntity)
}

