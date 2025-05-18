package com.fpf.smartscan.data.scans

import androidx.lifecycle.*
import androidx.room.*

@Dao
interface ScanDataDao {

    @Query("SELECT * FROM scan_data ORDER BY date DESC")
    fun getAllScanData(): LiveData<List<ScanData>>

    @Insert
    suspend fun insert(scanData: ScanData): Long

    @Query("DELETE FROM scan_data where id = :id")
    suspend fun delete(id: Int)

    @Query("UPDATE scan_data SET result = :result, date = :date WHERE id = :scanId")
    suspend fun update(scanId: Int, result: Int, date: Long)
}

