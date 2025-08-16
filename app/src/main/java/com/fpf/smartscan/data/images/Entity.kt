package com.fpf.smartscan.data.images

import androidx.room.*
import com.fpf.smartscan.lib.clip.ImageEmbedding

@Entity(tableName = "image_embeddings")
data class ImageEmbeddingEntity(
    @PrimaryKey
    val id: Long,     // Mediastore id
    val date: Long,
    val embeddings: FloatArray
)


fun ImageEmbeddingEntity.toEmbedding() = ImageEmbedding(id, date, embeddings)

fun ImageEmbedding.toEntity() = ImageEmbeddingEntity(id, date, embeddings)