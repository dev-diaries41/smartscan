package com.fpf.smartscan.data.scans

import androidx.lifecycle.*
import androidx.room.*

@Dao
interface ScanDataDao {

    @Query("SELECT * FROM scan_data ORDER BY date DESC")
    fun getAllScanData(): LiveData<List<ScanData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanData(scanData: ScanData)

    @Delete
    suspend fun deleteScanData(scanData: ScanData)
}

