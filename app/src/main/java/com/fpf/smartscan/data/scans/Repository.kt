package com.fpf.smartscan.data.scans

import androidx.lifecycle.*

class ScanDataRepository(private val dao: ScanDataDao) {
    val allScanData: LiveData<List<ScanData>> = dao.getAllScanData()

    suspend fun insert(scanData: ScanData) {
        dao.insertScanData(scanData)
    }

    suspend fun delete(scanData: ScanData) {
        dao.deleteScanData(scanData)
    }

    suspend fun getNextScanId(): Int {
        return dao.getHighestId() + 1
    }
}