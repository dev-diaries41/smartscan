package com.fpf.smartscan.data.videos

import androidx.lifecycle.LiveData

class VideoEmbeddingRepository(private val dao: VideoEmbeddingDao) {
    val allVideoEmbeddings: LiveData<List<VideoEmbedding>> = dao.getAllVideoEmbeddings()
    val hasAnyVideoEmbeddings: LiveData<Boolean> = dao.hasAnyVideoEmbeddings()

    suspend fun getAllEmbeddingsSync(): List<VideoEmbedding> {
        return dao.getAllEmbeddingsSync()
    }
    suspend fun insert(videoEmbedding: VideoEmbedding) {
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
}
