package com.fpf.smartscan.data.prototypes

import androidx.room.*
import com.fpf.smartscansdk.core.ml.embeddings.PrototypeEmbedding

@Entity(tableName = "prototype_embeddings")
data class PrototypeEmbeddingEntity(
    @PrimaryKey
    val id: String,
    val date: Long,
    val embeddings: FloatArray,
)

fun PrototypeEmbeddingEntity.toEmbedding() = PrototypeEmbedding(id, date, embeddings)

fun PrototypeEmbedding.toEntity() = PrototypeEmbeddingEntity(id, date, embeddings)

