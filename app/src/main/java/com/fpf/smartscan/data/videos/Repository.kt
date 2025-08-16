package com.fpf.smartscan.data.videos

import android.content.Context
import androidx.lifecycle.LiveData
import com.fpf.smartscan.lib.clip.Embedding
import com.fpf.smartscan.lib.clip.saveEmbeddingsToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoEmbeddingRepository(private val dao: VideoEmbeddingDao) {
    val allVideoEmbeddingsEntity: LiveData<List<VideoEmbeddingEntity>> = dao.getAllVideoEmbeddings()
    val hasAnyVideoEmbeddings: LiveData<Boolean> = dao.hasAnyVideoEmbeddings()

    suspend fun getAllEmbeddingsSync(): List<VideoEmbeddingEntity> {
        return dao.getAllEmbeddingsSync()
    }
    suspend fun insert(videoEmbedding: VideoEmbeddingEntity) {
        dao.insertVideoEmbedding(videoEmbedding)
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
