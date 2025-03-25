package com.fpf.smartscan.data.images

import androidx.room.*

@Entity(tableName = "image_embeddings")
data class ImageEmbedding(
    @PrimaryKey
    val id: Long,     // Mediastore id
    val date: Long,     // Timestamp of when the embedding was created
    val embeddings: FloatArray
)
