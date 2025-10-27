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

    // Tracking proměnné pro statistiky
    private var startTime: Long = 0
    private var tagsAssignedTotal = 0
    private val tagCountMap = mutableMapOf<String, MutableMap<String, Any>>() // name -> (count, color)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting image re-tagging")

            startTime = System.currentTimeMillis()

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

            // Načtení aktivních tagů pro stats
            val activeTags = getActiveTagsWithColors()

            // Progress callback pro notifikaci a WorkManager progress s rozšířenými statistikami
            suspend fun reportProgress(current: Int, total: Int) {
                try {
                    // Update foreground notification
                    setForegroundAsync(createForegroundInfo(current, total))

                    // Výpočet statistik
                    val elapsedMs = System.currentTimeMillis() - startTime
                    val avgTimePerImage = if (current > 0) elapsedMs.toFloat() / current.toFloat() else 0f
                    val imagesPerMinute = if (elapsedMs > 0) (current.toFloat() / (elapsedMs / 60000f)) else 0f

                    // Update tag counts - načtení aktuálního stavu z DB
                    updateTagCounts()

                    // Top 5 tagů
                    val topTagsJson = getTopTagsJson()

                    // Update WorkManager progress pro UI tracking s rozšířenými daty
                    setProgressAsync(workDataOf(
                        "current" to current,
                        "total" to total,
                        "tagsAssigned" to tagsAssignedTotal,
                        "activeTagsCount" to activeTags.size,
                        "topTags" to topTagsJson,
                        "avgTimePerImage" to avgTimePerImage,
                        "imagesPerMinute" to imagesPerMinute
                    ))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update notification: ${e.message}")
                }
            }

            val onProgress: (Int, Int) -> Unit = { current, total ->
                // Volání suspend funkce musí být v coroutine scope
                kotlinx.coroutines.runBlocking {
                    reportProgress(current, total)
                }
            }

            // Spuštění re-taggingu
            val totalTagsAssigned = taggingService.retagAllImages(
                embeddings = embeddings,
                onProgress = onProgress
            )

            tagsAssignedTotal = totalTagsAssigned

            Log.d(TAG, "Re-tagging complete: $totalTagsAssigned tags assigned to $totalImages images")

            // Notifikace o dokončení
            showCompletionNotification(totalImages, totalTagsAssigned)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Re-tagging failed", e)
            showErrorNotification(e.message)
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
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

    /**
     * Načte aktivní tagy s jejich barvami
     */
    private suspend fun getActiveTagsWithColors(): List<Pair<String, Int>> {
        return try {
            val database = com.fpf.smartscan.data.tags.TagDatabase.getDatabase(applicationContext)
            val userTags = database.userTagDao().getActiveTagsSync()
            userTags.map { it.name to it.color }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get active tags: ${e.message}")
            emptyList()
        }
    }

    /**
     * Update počty tagů z databáze
     */
    private suspend fun updateTagCounts() {
        try {
            val database = com.fpf.smartscan.data.tags.TagDatabase.getDatabase(applicationContext)
            val activeTags = database.userTagDao().getActiveTagsSync()

            var totalAssigned = 0
            activeTags.forEach { tag ->
                val count = database.imageTagDao().getImageCountForTag(tag.name)
                if (count > 0) {
                    tagCountMap[tag.name] = mutableMapOf(
                        "count" to count,
                        "color" to tag.color
                    )
                    totalAssigned += count
                }
            }

            tagsAssignedTotal = totalAssigned
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update tag counts: ${e.message}")
        }
    }

    /**
     * Získá top 5 tagů jako JSON string
     *
     * Formát: [{"name":"X","count":N,"color":C},...]
     */
    private fun getTopTagsJson(): String {
        return try {
            val sorted = tagCountMap.entries
                .sortedByDescending { (it.value["count"] as? Int) ?: 0 }
                .take(5)

            val items = sorted.map { (name, data) ->
                val count = data["count"] as? Int ?: 0
                val color = data["color"] as? Int ?: 0
                """{"name":"$name","count":$count,"color":$color}"""
            }

            "[${items.joinToString(",")}]"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate top tags JSON: ${e.message}")
            "[]"
        }
    }
}
