
package com.fpf.smartscan.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.fpf.smartscan.services.VideoIndexForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class VideoIndexWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = WorkerConstants.VIDEO_INDEXER_WORKER
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            applicationContext.startForegroundService(Intent(applicationContext,
                VideoIndexForegroundService::class.java))

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in orchestrator: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
}


fun scheduleVideoIndexWorker(context: Context, frequency: String, startImmediately: Boolean = true) {
    val (interval, unit) = when (frequency) {
        "1 Day" -> 1L to TimeUnit.DAYS
        "1 Week" -> 7L to TimeUnit.DAYS
        else -> {
            Log.e("scheduleVideoIndexWorker", "Invalid frequency: $frequency, defaulting to 1 Week")
            7L to TimeUnit.DAYS
        }
    }

    val builder = PeriodicWorkRequestBuilder<VideoIndexWorker>(interval, unit)

    if (!startImmediately) {
        builder.setInitialDelay(interval, unit)
    }

    val workRequest = builder.build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WorkerConstants.VIDEO_INDEXER_WORKER,
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
