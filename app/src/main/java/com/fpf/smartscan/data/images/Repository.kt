package com.fpf.smartscan.data.images

import android.content.Context
import androidx.lifecycle.LiveData
import com.fpf.smartscan.lib.clip.Embedding
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.saveEmbeddingsToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageEmbeddingRepository(private val dao: ImageEmbeddingDao) {
    val allImageEmbeddingsEntity: LiveData<List<ImageEmbeddingEntity>> = dao.getAllImageEmbeddings()
    val hasAnyEmbedding: LiveData<Boolean> = dao.hasAnyImageEmbedding()

    suspend fun getAllEmbeddingsSync(): List<ImageEmbeddingEntity> {
        return dao.getAllEmbeddingsSync()
    }
    suspend fun insert(imageEmbeddingEntity: ImageEmbeddingEntity) {
        dao.insertImageEmbedding(imageEmbeddingEntity)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteByIds(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            dao.deleteByIds(ids)
        }
    }

    suspend fun deleteAllEmbeddings() {
        dao.deleteAll()
    }

    suspend fun getAllEmbeddingsWithFileSync(context: Context, filename: String): List<Embedding> {
        val embeddings = dao.getAllEmbeddingsSync()
        val mappedEmbeddings = embeddings.map { it.toEmbedding() }
        if (mappedEmbeddings.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                saveEmbeddingsToFile(context, filename, mappedEmbeddings)
            }
        }
        return mappedEmbeddings
    }

}
