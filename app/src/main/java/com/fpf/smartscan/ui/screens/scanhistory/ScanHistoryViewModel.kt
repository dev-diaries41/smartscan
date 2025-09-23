package com.fpf.smartscan.ui.screens.scanhistory

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.fpf.smartscan.data.scans.*
import com.fpf.smartscan.data.movehistory.*
import com.fpf.smartscan.lib.moveFile
import com.fpf.smartscansdk.core.utils.MemoryOptions
import com.fpf.smartscansdk.core.utils.MemoryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

object Status {
    const val FAILED: String = "failed"
    const val IN_PROGRESS: String = "in progress"
    const val SUCCESS: String = "success"
}

class ScanHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val scansRepository: ScanDataRepository = ScanDataRepository(AppDatabase.getDatabase(application).scanDataDao())
    private val movesRepository: MoveHistoryRepository = MoveHistoryRepository(MoveHistoryDatabase.getDatabase(application).moveHistoryDao())
    val scanDataList: StateFlow<List<ScanData>> = scansRepository.allScanData.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _hasMoveHistoryForLastScan = MutableStateFlow<Boolean>(false)
    val hasMoveHistoryForLastScan: StateFlow<Boolean> = _hasMoveHistoryForLastScan

    private val _undoResultEvent = MutableStateFlow<String?>(null)
    val undoResultEvent: StateFlow<String?> = _undoResultEvent

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun checkHasMoveHistory(scanId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hasHistory = movesRepository.hasMoveHistory(scanId)
                _hasMoveHistoryForLastScan.emit(hasHistory)
            }catch (e: Exception){
            }
        }
    }

    fun undoLastScan(scanHistory: List<ScanData>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.emit(true)
                val lastScanId = scanHistory.maxOf { it.id }
                val movesForScan = movesRepository.getMoveHistory(lastScanId)
                val processedCount = AtomicInteger(0)
                val memoryUtils = MemoryUtils(getApplication(), MemoryOptions(minConcurrency = 2, maxConcurrency = 8))

                val batches = movesForScan.chunked(10)
                for (batch in batches) {
                    val concurrency = memoryUtils.calculateConcurrencyLevel()
                    val semaphore = Semaphore(concurrency)

                    val deferredResults = batch.map { move ->
                        async {
                            semaphore.withPermit {
                                try {
                                    val fileUri = move.sourceUri.toUri()
                                    val parentTreeUri = fileUri.toString()
                                        .substringBeforeLast("/document/")
                                        .toUri()

                                    val parentDir = DocumentFile.fromTreeUri(getApplication(), parentTreeUri)

                                    if(parentDir == null) throw IllegalArgumentException("Invalid parent dir")

                                    val resultUri = moveFile(
                                        getApplication(),
                                        move.destinationUri.toUri(),
                                        parentDir.uri
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

                _undoResultEvent.emit(message)
            } catch (e: Exception) {
                Log.e("ScanHistoryViewModel", "Error undoing last scan", e)
                _undoResultEvent.emit("Failed to undo scan")
            }finally {
                _isLoading.emit(false)
            }
        }
    }

}
