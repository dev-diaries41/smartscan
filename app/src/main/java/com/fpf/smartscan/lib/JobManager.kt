package com.fpf.smartscan.lib

import android.content.Context
import android.util.Log
import com.fpf.smartscan.data.jobResults.*

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

    suspend fun onComplete(jobName: String, startTime: Long, finishTime: Long, processedCount: Int) {
        val result = JobResult(
            jobName = jobName,
            startTime = startTime,
            finishTime = finishTime,
            processedCount = processedCount,
            isSuccess = true
        )
        insert(result)
    }

    suspend fun onError(jobName: String, startTime: Long, finishTime: Long, processedCount: Int) {
        val result = JobResult(
            jobName = jobName,
            startTime = startTime,
            finishTime = finishTime,
            processedCount = processedCount,
            isSuccess = false
        )
        insert(result)
    }

    suspend fun clearJobs(jobName: String) {
            repository.clearResultsByJobName(jobName)
            Log.i(TAG, "Jobs successfully cleared for: $jobName")
    }

    suspend fun getJobResults(jobName: String): Pair<Int, Pair<Long, Long>> {
        val totalProcessedCount = repository.getTotalProcessedCount(jobName)
        val results = repository.getAllResultsByJobName(jobName)

        if (results.isEmpty()) return Pair(0, Pair(0L, 0L))

        val startTime = results.minOf { it.startTime }
        val finishTime = results.maxOf { it.finishTime }
        return Pair(totalProcessedCount, Pair(startTime, finishTime))
    }

    private suspend fun insert(jobResult: JobResult) {
        repository.insertJobResult(jobResult)
    }
}