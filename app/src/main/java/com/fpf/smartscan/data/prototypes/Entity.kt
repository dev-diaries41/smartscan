package com.fpf.smartscan.data.prototypes

import androidx.room.*

@Entity(tableName = "prototype_embeddings")
data class PrototypeEmbedding(
    @PrimaryKey
    val id: String,
    val date: Long,
    val embeddings: FloatArray,
)
