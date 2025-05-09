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

    suspend fun delete(videoEmbedding: VideoEmbedding) {
        dao.deleteVideoEmbedding(videoEmbedding)
    }

    suspend fun isVideoIndexed(id: Long): Boolean {
        return dao.isVideoIndexed(id)
    }
}
