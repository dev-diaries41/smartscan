package com.fpf.smartscan.data.videos

import androidx.room.*
import com.fpf.smartscan.lib.clip.VideoEmbedding

@Entity(tableName = "video_embeddings")
data class VideoEmbeddingEntity(
    @PrimaryKey
    val id: Long,     // Mediastore id
    val date: Long,
    val embeddings: FloatArray
)

fun VideoEmbeddingEntity.toEmbedding() = VideoEmbedding(id, date, embeddings)

fun VideoEmbedding.toEntity() = VideoEmbeddingEntity(id, date, embeddings)