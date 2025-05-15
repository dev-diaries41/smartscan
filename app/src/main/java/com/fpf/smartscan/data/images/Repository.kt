package com.fpf.smartscan.data.images

import androidx.lifecycle.LiveData

class ImageEmbeddingRepository(private val dao: ImageEmbeddingDao) {
    val allImageEmbeddings: LiveData<List<ImageEmbedding>> = dao.getAllImageEmbeddings()
    val hasAnyEmbedding: LiveData<Boolean> = dao.hasAnyImageEmbedding()

    suspend fun getAllEmbeddingsSync(): List<ImageEmbedding> {
        return dao.getAllEmbeddingsSync()
    }
    suspend fun insert(imageEmbedding: ImageEmbedding) {
        dao.insertImageEmbedding(imageEmbedding)
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
}
