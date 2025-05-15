package com.fpf.smartscan.data.videos

import androidx.room.*

@Entity(tableName = "video_embeddings")
data class VideoEmbedding(
    @PrimaryKey
    val id: Long,     // Mediastore id
    val date: Long,     // Timestamp of when the embedding was created
    val embeddings: FloatArray
)
