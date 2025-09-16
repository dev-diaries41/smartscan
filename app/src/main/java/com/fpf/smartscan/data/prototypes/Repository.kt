package com.fpf.smartscan.data.prototypes

import kotlinx.coroutines.flow.Flow


class PrototypeEmbeddingRepository(private val dao: PrototypeEmbeddingDao) {
    val allEmbeddings: Flow<List<PrototypeEmbeddingEntity>> = dao.getAllEmbeddings()

    suspend fun getAllEmbeddingsSync(): List<PrototypeEmbeddingEntity> {
        return dao.getAllEmbeddingsSync()
    }

    suspend fun insert(prototype: PrototypeEmbeddingEntity) {
        dao.insertPrototype(prototype)
    }

    suspend fun delete(prototype: PrototypeEmbeddingEntity) {
        dao.deletePrototype(prototype)
    }
}
