package com.fpf.smartscan.data.images

import androidx.room.*
import com.fpf.smartscansdk.core.ml.embeddings.Embedding

@Entity(tableName = "image_embeddings")
data class ImageEmbeddingEntity(
    @PrimaryKey
    val id: Long,     // Mediastore id
    val date: Long,
    val embeddings: FloatArray
)


fun ImageEmbeddingEntity.toEmbedding() = Embedding(id, date, embeddings)

fun Embedding.toEntity() = ImageEmbeddingEntity(id, date, embeddings)