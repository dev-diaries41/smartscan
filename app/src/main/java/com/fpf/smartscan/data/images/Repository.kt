package com.fpf.smartscan.data.images

import androidx.lifecycle.LiveData
import com.fpf.smartscan.lib.clip.Embedding
import com.fpf.smartscan.lib.clip.saveEmbeddingsToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ImageEmbeddingRepository(private val dao: ImageEmbeddingDao) {
    val hasAnyEmbedding: LiveData<Boolean> = dao.hasAnyImageEmbedding()

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
