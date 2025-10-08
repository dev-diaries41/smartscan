package com.fpf.smartscan.data.movehistory

class MoveHistoryRepository(private val dao: MoveHistoryDao) {

    suspend fun getMoveHistory(scanId: Int): List<MoveHistory> {
        return dao.getMoveHistory(scanId)
    }

    suspend fun hasMoveHistory(scanId: Int): Boolean {
        return dao.hasMoveHistory(scanId)
    }

    suspend fun insert(move: MoveHistory) {
        dao.insertMove(move)
    }

    suspend fun deleteMoveHistory(scanId: Int) {
        dao.deleteMoveHistory(scanId)
    }

    suspend fun clear(){
        dao.clear()
    }
}
