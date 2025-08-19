package com.fpf.smartscan.data.prototypes

import kotlinx.coroutines.flow.Flow


class PrototypeEmbeddingRepository(private val dao: PrototypeEmbeddingDao) {
    val allEmbeddings: Flow<List<PrototypeEmbedding>> = dao.getAllEmbeddings()

    suspend fun getAllEmbeddingsSync(): List<PrototypeEmbedding> {
        return dao.getAllEmbeddingsSync()
    }

    suspend fun insert(prototype: PrototypeEmbedding) {
        dao.insertPrototype(prototype)
    }

    suspend fun delete(prototype: PrototypeEmbedding) {
        dao.deletePrototype(prototype)
    }
}
