package com.fpf.smartscan.lib.clip

sealed interface Embedding {
    val id: Long
    val date: Long
    val embeddings: FloatArray
}

data class ImageEmbedding(
    override val id: Long,
    override val date: Long,
    override val embeddings: FloatArray
) : Embedding

data class VideoEmbedding(
    override val id: Long,
    override val date: Long,
    override val embeddings: FloatArray
) : Embedding
