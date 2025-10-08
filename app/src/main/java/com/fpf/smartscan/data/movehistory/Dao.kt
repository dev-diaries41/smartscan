package com.fpf.smartscan.data.movehistory

import androidx.room.*

@Dao
interface MoveHistoryDao {

    @Query("SELECT * FROM move_history  WHERE scanId = :scanId ORDER BY id DESC")
    fun getMoveHistory(scanId: Int): List<MoveHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMove(moveHistory: MoveHistory)

    @Query("DELETE FROM move_history WHERE scanId = :scanId")
    suspend fun deleteMoveHistory(scanId: Int)

    @Query("DELETE FROM move_history")
    suspend fun clear()


    @Query("SELECT EXISTS(SELECT 1 FROM move_history where scanId = :scanId)")
    fun hasMoveHistory(scanId: Int): Boolean
}

