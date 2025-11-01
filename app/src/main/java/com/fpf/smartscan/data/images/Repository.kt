package com.fpf.smartscan.data.images

import com.fpf.smartscansdk.core.data.Embedding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ImageEmbeddingRepository(private val dao: ImageEmbeddingDao) {
    val hasAnyEmbedding: Flow<Boolean> = dao.hasAnyImageEmbedding()

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
