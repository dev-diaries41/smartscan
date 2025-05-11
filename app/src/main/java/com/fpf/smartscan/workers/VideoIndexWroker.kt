
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
        private const val TAG = "VideoIndexWorker"
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


fun scheduleVideoIndexWorker(context: Context, frequency: String) {
    val duration = when (frequency) {
        "1 Day" -> 1L to TimeUnit.DAYS
        "1 Week" -> 7L to TimeUnit.DAYS
        else -> {
            Log.e("scheduleVideoIndexWorker", "Invalid frequency: $frequency, defaulting to 1 Week")
            7L to TimeUnit.DAYS
        }
    }

    val workRequest = PeriodicWorkRequestBuilder<VideoIndexWorker>(duration.first, duration.second)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "VideoIndexWorker",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
