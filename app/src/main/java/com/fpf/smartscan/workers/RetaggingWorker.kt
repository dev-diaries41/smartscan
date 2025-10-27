package com.fpf.smartscan.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import com.fpf.smartscan.R
import com.fpf.smartscan.services.TaggingService
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.CLIP_EMBEDDING_LENGTH
import com.fpf.smartscansdk.extensions.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.extensions.indexers.ImageIndexer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker pro re-tagging všech již indexovaných obrázků
 *
 * Používá se po:
 * - Vytvoření nového tagu
 * - Změně threshold u existujícího tagu
 * - Změně description (a tedy embeddingu) tagu
 *
 * Worker načte všechny embeddingy z FileEmbeddingStore a pro každý
 * zavolá TaggingService.assignTags()
 */
class RetaggingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "RetaggingWorker"
        const val WORK_NAME = "retag_images_work"
        private const val NOTIFICATION_ID = 300
        private const val CHANNEL_ID = "retagging_channel"
    }

    private val taggingService = TaggingService(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting image re-tagging")

            // Nastavení foreground notifikace
            setForeground(createForegroundInfo(0, 0))

            // Načtení image embeddingů z file store
            val imageStore = FileEmbeddingStore(
                applicationContext.filesDir,
                ImageIndexer.INDEX_FILENAME,
                CLIP_EMBEDDING_LENGTH
            )

            if (!imageStore.exists) {
                Log.w(TAG, "No image index found, nothing to retag")
                return@withContext Result.success()
            }

            val embeddings = imageStore.get()
            val totalImages = embeddings.size

            if (totalImages == 0) {
                Log.w(TAG, "Image index is empty, nothing to retag")
                return@withContext Result.success()
            }

            Log.d(TAG, "Re-tagging $totalImages images")

            // Progress callback pro notifikaci a WorkManager progress
            val onProgress: (Int, Int) -> Unit = { current, total ->
                try {
                    // Update foreground notification
                    setForegroundAsync(createForegroundInfo(current, total))

                    // Update WorkManager progress pro UI tracking
                    setProgressAsync(workDataOf(
                        "current" to current,
                        "total" to total
                    ))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update notification: ${e.message}")
                }
            }

            // Spuštění re-taggingu
            val totalTagsAssigned = taggingService.retagAllImages(
                embeddings = embeddings,
                onProgress = onProgress
            )

            Log.d(TAG, "Re-tagging complete: $totalTagsAssigned tags assigned to $totalImages images")

            // Notifikace o dokončení
            showCompletionNotification(totalImages, totalTagsAssigned)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Re-tagging failed", e)
            showErrorNotification(e.message)
            Result.failure()
        }
    }

    /**
     * Vytvoří ForegroundInfo pro zobrazení notifikace během běhu
     */
    private fun createForegroundInfo(current: Int, total: Int): ForegroundInfo {
        createNotificationChannel()

        val title = applicationContext.getString(R.string.notif_title_retagging)
        val text = if (total > 0) {
            "Zpracováno $current z $total obrázků"
        } else {
            "Příprava re-taggingu..."
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.smartscan_logo)
            .setOngoing(true)
            .setProgress(total, current, total == 0)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    /**
     * Vytvoří notifikační kanál
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Re-tagging obrázků",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifikace pro automatické přiřazování tagů"
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Zobrazí notifikaci o dokončení
     */
    private fun showCompletionNotification(imagesProcessed: Int, tagsAssigned: Int) {
        val title = "Re-tagging dokončen"
        val text = "Zpracováno $imagesProcessed obrázků, přiřazeno $tagsAssigned tagů"

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.smartscan_logo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Zobrazí error notifikaci
     */
    private fun showErrorNotification(errorMessage: String?) {
        val title = "Re-tagging selhal"
        val text = errorMessage ?: "Neznámá chyba"

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.smartscan_logo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID + 2, notification)
    }
}
