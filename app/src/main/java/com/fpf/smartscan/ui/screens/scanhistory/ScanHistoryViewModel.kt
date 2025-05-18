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
import com.fpf.smartscan.lib.MemoryUtils
import com.fpf.smartscan.lib.moveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

class ScanHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val scansRepository: ScanDataRepository = ScanDataRepository(AppDatabase.getDatabase(application).scanDataDao())
    private val movesRepository: MoveHistoryRepository = MoveHistoryRepository(MoveHistoryDatabase.getDatabase(application).moveHistoryDao())
    val scanDataList: LiveData<List<ScanData>> = scansRepository.allScanData

    private val _hasMoveHistoryForLastScan = MutableLiveData<Boolean>(false)
    val hasMoveHistoryForLastScan: LiveData<Boolean> = _hasMoveHistoryForLastScan

    private val _undoResultEvent = MutableLiveData<String?>(null)
    val undoResultEvent: LiveData<String?> = _undoResultEvent

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun checkHasMoveHistory(scanId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hasHistory = movesRepository.hasMoveHistory(scanId)
                _hasMoveHistoryForLastScan.postValue(hasHistory)
            }catch (e: Exception){
            }
        }
    }

    fun undoLastScan(scanHistory: List<ScanData>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                val lastScanId = scanHistory.maxOf { it.id }
                val movesForScan = movesRepository.getMoveHistory(lastScanId)
                val processedCount = AtomicInteger(0)
                val memoryUtils = MemoryUtils(getApplication(), minConcurrency = 2, maxConcurrency = 8)

                val batches = movesForScan.chunked(10)
                for (batch in batches) {
                    val concurrency = memoryUtils.calculateConcurrencyLevel()
                    val semaphore = Semaphore(concurrency)

                    val deferredResults = batch.map { move ->
                        async {
                            semaphore.withPermit {
                                try {
                                    val resultUri = moveFile(
                                        getApplication(),
                                        move.destinationUri.toUri(),
                                        move.sourceUri.toUri()
                                    )
                                    if (resultUri != null) {
                                        processedCount.incrementAndGet()
                                        true
                                    } else {
                                        false
                                    }
                                } catch (e: Exception) {
                                    Log.e("ScanHistoryViewModel", "Failed to move back ${move.destinationUri}", e)
                                    false
                                }
                            }
                        }
                    }

                    deferredResults.awaitAll()
                }

                val total = movesForScan.size
                val success = processedCount.get()
                val failures = total - success

                val message = when {
                    failures == 0 -> "$success images restored"
                    success == 0 -> "Failed to undo scan"
                    else -> "$success images restored, $failures failed"
                }

                movesRepository.deleteMoveHistory(lastScanId)
                scansRepository.delete(lastScanId)

                _undoResultEvent.postValue(message)
            } catch (e: Exception) {
                Log.e("ScanHistoryViewModel", "Error undoing last scan", e)
                _undoResultEvent.postValue("Failed to undo scan")
            }finally {
                _isLoading.postValue(false)
            }
        }
    }

}
