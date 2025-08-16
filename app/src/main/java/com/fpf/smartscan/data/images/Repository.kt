package com.fpf.smartscan.data.images

import androidx.lifecycle.LiveData

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
}
