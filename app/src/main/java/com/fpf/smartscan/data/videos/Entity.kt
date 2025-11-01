package com.fpf.smartscan.data.videos

import androidx.room.*
import com.fpf.smartscansdk.core.data.Embedding

@Entity(tableName = "video_embeddings")
data class VideoEmbeddingEntity(
    @PrimaryKey
    val id: Long,     // Mediastore id
    val date: Long,
    val embeddings: FloatArray
)

fun VideoEmbeddingEntity.toEmbedding() = Embedding(id, date, embeddings)

fun Embedding.toEntity() = VideoEmbeddingEntity(id, date, embeddings)