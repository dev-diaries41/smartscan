package com.fpf.smartscan.data.scans

import kotlinx.coroutines.flow.Flow

class ScanDataRepository(private val dao: ScanDataDao) {
    val allScanData: Flow<List<ScanData>> = dao.getAllScanData()

    suspend fun insert(scanData: ScanData): Int {
        return dao.insert(scanData).toInt()
    }

    suspend fun update(scanId: Int, result: Int, date: Long) {
        dao.update(scanId, result, date)
    }

    suspend fun delete(id: Int) {
        dao.delete(id)
    }

    suspend fun clear(){
        dao.clear()
    }

}