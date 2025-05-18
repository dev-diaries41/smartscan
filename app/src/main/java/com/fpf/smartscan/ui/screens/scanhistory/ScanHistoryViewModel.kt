package com.fpf.smartscan.ui.screens.scanhistory

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.fpf.smartscan.data.scans.*
import com.fpf.smartscan.data.movehistory.*
import com.fpf.smartscan.lib.moveFile
import kotlinx.coroutines.Dispatchers

class ScanHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val scansRepository: ScanDataRepository = ScanDataRepository(AppDatabase.getDatabase(application).scanDataDao())
    private val movesRepository: MoveHistoryRepository = MoveHistoryRepository(MoveHistoryDatabase.getDatabase(application).moveHistoryDao())
    private val _hasMoveHistoryForLastScan = MutableLiveData<Boolean>(false)
    val hasMoveHistoryForLastScan: LiveData<Boolean> = _hasMoveHistoryForLastScan
    val scanDataList: LiveData<List<ScanData>> = scansRepository.allScanData
    private val _undoResultEvent = MutableLiveData<String?>(null)
    val undoResultEvent: LiveData<String?> = _undoResultEvent

    fun checkHasMoveHistory(scanId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hasHistory = movesRepository.hasMoveHistory(scanId)
                _hasMoveHistoryForLastScan.postValue(hasHistory)
            }catch (e: Exception){
            }
        }
    }

    fun undoLastScan(scanHistory: List<ScanData>){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lastScanId = scanHistory.maxOf { item -> item.id }
                val movesForScan = movesRepository.getMoveHistory(lastScanId)
                val results = movesForScan.map {
                    moveFile(getApplication(), it.destinationUri.toUri(), it.sourceUri.toUri())
                }
                val successfulMoves = results.count{it!=null}
                val failedMoves = results.size - successfulMoves

                val message = when {
                    failedMoves == 0 -> "$successfulMoves images restored"
                    (successfulMoves == 0) && movesForScan.isNotEmpty() -> "Failed to undo scan"
                    else -> "$successfulMoves images restored, $failedMoves failed"
                }
                _undoResultEvent.postValue(message)
                movesRepository.deleteMoveHistory(lastScanId)
                scansRepository.delete(lastScanId)
            }catch (e: Exception){
                val errorMessage = "Failed to undo scan"
                _undoResultEvent.postValue(errorMessage)
                Log.e("ScanHistoryViewModel", "Error undoing last scan: $e")
            }
        }
    }
}
