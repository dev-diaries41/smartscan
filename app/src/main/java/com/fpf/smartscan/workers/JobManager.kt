package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.util.Log
import com.fpf.smartscan.R
import com.fpf.smartscan.data.jobResults.*
import com.fpf.smartscan.data.scans.AppDatabase
import com.fpf.smartscan.data.scans.ScanData
import com.fpf.smartscan.data.scans.ScanDataRepository
import com.fpf.smartscan.lib.getTimeInMinutesAndSeconds
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class StartResult(
    val startTime: Long,
    val initialProcessedCount: Int
)

class JobManager private constructor(context: Context) {

    private val db = JobsDatabase.getInstance(context.applicationContext)
    private val repository = JobResultRepository(db.jobResultDao())

    companion object {
        @Volatile private var INSTANCE: JobManager? = null
        private const val TAG = "JobManager"

        fun getInstance(context: Context): JobManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: JobManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    suspend fun onStart(jobName: String): StartResult {
        val startTime = System.currentTimeMillis()
        val previousProcessedCount = repository.getTotalProcessedCount(jobName)
        return StartResult(startTime, previousProcessedCount)
    }

    fun onComplete(jobName: String, startTime: Long, finishTime: Long, processedCount: Int) {
        val result = JobResult(
            jobName = jobName,
            startTime = startTime,
            finishTime = finishTime,
            processedCount = processedCount,
            isSuccess = true
        )
        insert(result)
    }

    fun onError(jobName: String, startTime: Long, finishTime: Long, processedCount: Int) {
        val result = JobResult(
            jobName = jobName,
            startTime = startTime,
            finishTime = finishTime,
            processedCount = processedCount,
            isSuccess = false
        )
        insert(result)
    }

    fun clearJobs(jobName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.clearResultsByJobName(jobName)
            Log.i(TAG, "Jobs successfully cleared for: $jobName")
        }
    }

    suspend fun onAllIndexJobsComplete(context: Context, jobName: String) {
        val (totalProcessedCount, timePair) = getProcessingInfo(jobName)
        if (totalProcessedCount == 0) return

        val totalProcessingTime = timePair.second - timePair.first
        val (minutes, seconds) = getTimeInMinutesAndSeconds(totalProcessingTime)
        val notificationText = "Total images indexed: $totalProcessedCount, Time: ${minutes}m ${seconds}s"

        showNotification(context, context.getString(R.string.notif_title_index_complete), notificationText, 1002)
    }

    suspend fun onAllClassificationJobsComplete(context: Context, jobName: String) {
        val (totalProcessedCount, timePair) = getProcessingInfo(jobName)
        if (totalProcessedCount == 0) return

        val totalProcessingTime = timePair.second - timePair.first
        val (minutes, seconds) = getTimeInMinutesAndSeconds(totalProcessingTime)
        val notificationText = "Total images moved: $totalProcessedCount, Time: ${minutes}m ${seconds}s"

        insertScanData(context, totalProcessedCount)
        showNotification(context, context.getString(R.string.notif_title_smart_scan_complete), notificationText, 1003)
    }

    private suspend fun insertScanData(context: Context, processedImages: Int){
        // Load in function since only needed on time and only for classification
        val repository = ScanDataRepository(AppDatabase.getDatabase(context.applicationContext as Application).scanDataDao())
        try {
            repository.insert(
                ScanData(result=processedImages, date = System.currentTimeMillis())
            )
        }
        catch (e: Exception){
            Log.e(TAG, "Error inserting scan data: ${e.message}", e)
        }
    }

    private suspend fun getProcessingInfo(jobName: String): Pair<Int, Pair<Long, Long>> {
        val totalProcessedCount = repository.getTotalProcessedCount(jobName)
        val results = repository.getAllResultsByJobName(jobName)

        if (results.isEmpty()) return Pair(0, Pair(0L, 0L))

        val startTime = results.minOf { it.startTime }
        val finishTime = results.maxOf { it.finishTime }

        return Pair(totalProcessedCount, Pair(startTime, finishTime))
    }

    private fun insert(jobResult: JobResult) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.insertJobResult(jobResult)
        }
    }
}
