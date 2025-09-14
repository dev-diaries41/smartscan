package com.fpf.smartscan.data.videos


import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VideoEmbeddingRepository(private val dao: VideoEmbeddingDao) {
    val hasAnyVideoEmbeddings: Flow<Boolean> = dao.hasAnyVideoEmbeddings()

    suspend fun getAllEmbeddingsWithFileSync(): List<Embedding> {
        val embeddings = dao.getAllEmbeddingsSync()
        val mappedEmbeddings = embeddings.map { it.toEmbedding() }
        if (mappedEmbeddings.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                dao.deleteAll() // room db no longer needed
            }
        }
        return mappedEmbeddings
    }
}
