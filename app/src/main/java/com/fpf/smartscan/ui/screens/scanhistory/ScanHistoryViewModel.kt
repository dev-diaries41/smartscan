package com.fpf.smartscan.ui.screens.scanhistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.fpf.smartscan.data.scans.*

class ScanHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanDataRepository
    val scanDataList: LiveData<List<ScanData>>

    init {
        val database = AppDatabase.getDatabase(application)
        val dao = database.scanDataDao()
        repository = ScanDataRepository(dao)
        scanDataList = repository.allScanData
    }

    fun deleteScanData(scanData: ScanData) = viewModelScope.launch {
        repository.delete(scanData)
    }
}
