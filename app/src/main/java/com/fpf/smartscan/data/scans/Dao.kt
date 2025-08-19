package com.fpf.smartscan.data.scans

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDataDao {

    @Query("SELECT * FROM scan_data ORDER BY date DESC")
    fun getAllScanData(): Flow<List<ScanData>>

    @Insert
    suspend fun insert(scanData: ScanData): Long

    @Query("DELETE FROM scan_data where id = :id")
    suspend fun delete(id: Int)

    @Query("UPDATE scan_data SET result = :result, date = :date WHERE id = :scanId")
    suspend fun update(scanId: Int, result: Int, date: Long)
}

