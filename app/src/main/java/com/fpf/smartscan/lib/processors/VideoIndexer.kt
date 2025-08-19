package com.fpf.smartscan.lib.processors

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscan.R
import com.fpf.smartscan.lib.IMAGE_SIZE_X
import com.fpf.smartscan.lib.IMAGE_SIZE_Y
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.MemoryUtils
import com.fpf.smartscan.lib.clip.Embedding
import com.fpf.smartscan.lib.clip.appendEmbeddingsToFile
import com.fpf.smartscan.lib.clip.loadEmbeddingsFromFile
import com.fpf.smartscan.lib.clip.saveEmbeddingsToFile
import com.fpf.smartscan.lib.getTimeInMinutesAndSeconds
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class VideoIndexer(
    private val application: Application,
    private val listener: IIndexListener? = null
) {
    companion object {
        private const val TAG = "VideoIndexer"
        private const val BATCH_SIZE = 10
        const val INDEX_FILENAME = "video_index.bin"
    }



    suspend fun run(ids: List<Long>, embeddingHandler: Embeddings): Int = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        try {
            if (ids.isEmpty()) {
                Log.d(TAG, "No videos to index.")
                return@withContext 0
            }
            val file = File(application.filesDir, INDEX_FILENAME)

            val existingIds: Set<Long> = if(file.exists()){ loadEmbeddingsFromFile(file)
                .map { it.id }
                .toSet()
            } else emptySet<Long>()
            val videosToProcess = ids.filterNot { existingIds.contains(it) }
            val idsToPurge = existingIds.minus(ids.toSet()).toList()

            var totalProcessed = 0

            val memoryUtils = MemoryUtils(application)

            for (batch in videosToProcess.chunked(10)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                // Log.i(TAG, "Current allowed concurrency: $currentConcurrency | Free Memory: ${memoryUtils.getFreeMemory() / (1024 * 1024)} MB")

                val semaphore = Semaphore(currentConcurrency)
                val batchEmb = ArrayList<Embedding>(BATCH_SIZE)

                val deferredResults = batch.map { id ->
                    async {
                        semaphore.withPermit {
                            try {
                                val contentUri = ContentUris.withAppendedId(
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                                )
                                val frameBitmaps = extractFramesFromVideo(application, contentUri)

                                if(frameBitmaps == null) return@async 0

                                val embedding: FloatArray = embeddingHandler.generatePrototypeEmbedding(application, frameBitmaps)

                                batchEmb.add(
                                    Embedding(
                                        id = id,
                                        date = System.currentTimeMillis(),
                                        embeddings = embedding
                                    )
                                )
                                val current = processedCount.incrementAndGet()
                                listener?.onProgress(current, videosToProcess.size)
                                return@async 1
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to process video $id", e)
                            }
                            return@async 0
                        }
                    }
                }
                totalProcessed += deferredResults.awaitAll().sum()
                appendEmbeddingsToFile(file, batchEmb)
            }
            val endTime = System.currentTimeMillis()
            val completionTime = endTime - startTime
            listener?.onComplete(application, totalProcessed, completionTime)
            purge(idsToPurge, file)
            totalProcessed
        }
        catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
            listener?.onError(application, e)
            Log.e(TAG, "Error indexing videos: ${e.message}", e)
            0
        }
    }

    private fun extractFramesFromVideo(context: Context, videoUri: Uri, frameCount: Int = 10): List<Bitmap>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)

            val durationUs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.times(1000)
                ?: return null

            val frameList = mutableListOf<Bitmap>()

            for (i in 0 until frameCount) {
                val frameTimeUs = (i * durationUs) / frameCount
                val bitmap = retriever.getScaledFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    IMAGE_SIZE_X, IMAGE_SIZE_Y
                )

                if (bitmap != null) {
                    frameList.add(bitmap)
                } else {
                    // Temporary Fix: Break early if null which suggest codec issue with video
                    break
                }
            }

            if (frameList.isEmpty()) return null

            frameList
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting video frames: $e")
            null
        } finally {
            retriever.release()
        }
    }

    private suspend fun purge(idsToPurge: List<Long>, file: File) = withContext(Dispatchers.IO) {
        if (idsToPurge.isEmpty()) return@withContext

        try {
            val embs = loadEmbeddingsFromFile(file)
            val remaining = embs.filter { it.id !in idsToPurge }
            saveEmbeddingsToFile(file, remaining)
            Log.i(TAG, "Purged ${idsToPurge.size} stale embeddings")
        } catch (e: Exception) {
            Log.e(TAG, "Error purging embeddings", e)
        }
    }

}

object VideoIndexListener : IIndexListener {
    const val NOTIFICATION_ID = 1003
    const val TAG = "VideoIndexListener"
    private val _progress = MutableStateFlow<Float>(0f)
    val progress: StateFlow<Float> = _progress

    private val _indexingStatus = MutableStateFlow<IndexStatus>(IndexStatus.IDLE)
    val indexingStatus: StateFlow<IndexStatus> = _indexingStatus

    override fun onProgress(processedCount: Int, total: Int) {
        if(_indexingStatus.value != IndexStatus.INDEXING){
            _indexingStatus.value = IndexStatus.INDEXING
        }
        val currentProgress = processedCount.toFloat() / total.toFloat()
        if(currentProgress - _progress.value >= 0.01f){
            _progress.value = currentProgress
        }
        else if(processedCount == total){
            _progress.value = 1f
        }
    }

    override fun onComplete(context: Context, totalProcessed: Int, processingTime: Long) {
        if (totalProcessed == 0) return

        try {
            _indexingStatus.value = IndexStatus.COMPLETE
            _progress.value = 0f
            val (minutes, seconds) = getTimeInMinutesAndSeconds(processingTime)
            val notificationText = "Total videos indexed: ${totalProcessed}, Time: ${minutes}m ${seconds}s"
            showNotification(context, context.getString(R.string.notif_title_index_complete), notificationText, NOTIFICATION_ID)
        }
        catch (e: Exception){
            Log.e(TAG, "Error in onComplete: ${e.message}", e)
        }
    }

    override fun onError(context: Context, error: Exception) {
        try {
            _indexingStatus.value = IndexStatus.ERROR
            val title = context.getString(R.string.notif_title_index_error_service, "Video")
            val content = context.getString(R.string.notif_content_index_error_service, "video")
            showNotification(context, title, content, NOTIFICATION_ID)
        }
        catch (e: Exception){
            Log.e(TAG, "Error in onError: ${e.message}", e)
        }
    }
}
