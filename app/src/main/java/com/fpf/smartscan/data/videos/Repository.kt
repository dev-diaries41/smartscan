package com.fpf.smartscan.data.videos

import com.fpf.smartscan.lib.clip.Embedding
import com.fpf.smartscan.lib.clip.saveEmbeddingsToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class VideoEmbeddingRepository(private val dao: VideoEmbeddingDao) {
    val hasAnyVideoEmbeddings: Flow<Boolean> = dao.hasAnyVideoEmbeddings()

    suspend fun getAllEmbeddingsWithFileSync(file: File): List<Embedding> {
        val embeddings = dao.getAllEmbeddingsSync()
        val mappedEmbeddings = embeddings.map { it.toEmbedding() }
        if (mappedEmbeddings.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                saveEmbeddingsToFile(file, mappedEmbeddings)
                dao.deleteAll() // room db no longer needed
            }
        }
        return mappedEmbeddings
    }
}
