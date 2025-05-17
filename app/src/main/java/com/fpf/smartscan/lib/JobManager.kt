package com.fpf.smartscan.lib

import android.content.Context
import android.util.Log
import com.fpf.smartscan.data.jobs.*

data class StartResult(
    val startTime: Long,
    val initialProcessedCount: Int
)

data class JobResults(
    val totalProcessedCount: Int,
    val startTime: Long,
    val finishTime: Long,
    val errorCount: Int
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

    suspend fun getJobResults(jobName: String): JobResults {
        val totalProcessedCount = repository.getTotalProcessedCount(jobName)
        val results = repository.getAllResultsByJobName(jobName)

        if (results.isEmpty()) return JobResults(0, 0L, 0L, 0)

        val startTime = results.minOf { it.startTime }
        val finishTime = results.maxOf { it.finishTime }
        val errorCount = results.count { !it.isSuccess }
        return JobResults(totalProcessedCount, startTime, finishTime, errorCount)
    }

    private suspend fun insert(jobResult: JobResult) {
        repository.insertJobResult(jobResult)
    }
}