
package com.fpf.smartscan.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.fpf.smartscan.services.ImageIndexForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ImageIndexWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = WorkerConstants.IMAGE_INDEXER_WORKER
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            applicationContext.startForegroundService(
                Intent(
                    applicationContext,
                    ImageIndexForegroundService::class.java
                )
            )

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in orchestrator: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
}

fun scheduleImageIndexWorker(context: Context, frequency: String) {
    val duration = when (frequency) {
        "1 Day" -> 1L to TimeUnit.DAYS
        "1 Week" -> 7L to TimeUnit.DAYS
        else -> {
            Log.e("scheduleImageIndexWorker", "Invalid frequency: $frequency, defaulting to 1 Week")
            7L to TimeUnit.DAYS
        }
    }

    val workRequest = PeriodicWorkRequestBuilder<ImageIndexWorker>(duration.first, duration.second)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WorkerConstants.IMAGE_INDEXER_WORKER,
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}


