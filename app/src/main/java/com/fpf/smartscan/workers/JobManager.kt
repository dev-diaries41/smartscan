package com.fpf.smartscan.workers

import android.content.Context
import android.util.Log
import com.fpf.smartscan.data.jobResults.*
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class StartResult(
    val startTime: Long,
    val initialProcessedCount: Int
)

class JobManager private constructor(context: Context) {

    private val db = JobsDatabase.getInstance(context)
    private val repository = JobResultRepository(db.jobResultDao())

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

    suspend fun notifyAllJobsComplete(context: Context, jobName: String) {
        val totalProcessedCount = repository.getTotalProcessedCount(jobName)
        val results = repository.getAllResultsByJobName(jobName)

        if (results.isEmpty()) return

        val startTime = results.minOf { it.startTime }
        val finishTime = results.maxOf { it.finishTime }
        val totalProcessingTime = finishTime - startTime

        val processingTimeSeconds = totalProcessingTime / 1000
        val minutes = processingTimeSeconds / 60
        val seconds = processingTimeSeconds % 60

        val notificationText = "Total $jobName jobs processed: $totalProcessedCount, Time: ${minutes}m ${seconds}s"
        Log.i(TAG, notificationText)

        showNotification(context, "$jobName Complete", notificationText, 1002)
    }


    private fun insert(jobResult: JobResult) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.insertJobResult(jobResult)
        }
    }

    companion object {
        @Volatile private var INSTANCE: JobManager? = null
        private const val TAG = "JobManager"

        fun getInstance(context: Context): JobManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: JobManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
