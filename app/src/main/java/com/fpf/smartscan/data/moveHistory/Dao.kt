package com.fpf.smartscan.data.moveHistory

import androidx.room.*

@Dao
interface MoveHistoryDao {

    @Query("SELECT * FROM move_history  WHERE scanId = :scanId ORDER BY id DESC")
    fun getMoveHistory(scanId: Long): List<MoveHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMove(moveHistory: MoveHistory)

    @Query("DELETE FROM move_history WHERE scanId = :scanId")
    suspend fun deleteMoveHistory(scanId: Long)
}

