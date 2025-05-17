package com.fpf.smartscan.data.movehistory

class MoveHistoryRepository(private val dao: MoveHistoryDao) {

    suspend fun getMoveHistory(scanId: Int): List<MoveHistory> {
        return dao.getMoveHistory(scanId)
    }

    suspend fun insert(move: MoveHistory) {
        dao.insertMove(move)
    }

    suspend fun deleteMoveHistory(scanId: Int) {
        dao.deleteMoveHistory(scanId)
    }
}
