package com.fpf.smartscan.data.images

import androidx.lifecycle.LiveData

class ImageEmbeddingRepository(private val dao: ImageEmbeddingDao) {
    val allImageEmbeddings: LiveData<List<ImageEmbedding>> = dao.getAllImageEmbeddings()

    suspend fun getAllEmbeddingsSync(): List<ImageEmbedding> {
        return dao.getAllEmbeddingsSync()
    }
    suspend fun insert(imageEmbedding: ImageEmbedding) {
        dao.insertImageEmbedding(imageEmbedding)
    }

    suspend fun delete(imageEmbedding: ImageEmbedding) {
        dao.deleteImageEmbedding(imageEmbedding)
    }

    suspend fun isImageIndexed(id: Long): Boolean {
        return dao.isImageIndexed(id)
    }

    suspend fun deleteAllEmbeddings() {
        dao.deleteAll()
    }
}
