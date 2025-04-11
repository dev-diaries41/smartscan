package com.fpf.smartscan.lib.processors

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.fpf.smartscan.data.prototypes.PrototypeEmbedding
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingDatabase
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.clip.getSimilarities
import com.fpf.smartscan.lib.clip.getTopN
import com.fpf.smartscan.lib.getBitmapFromUri
import com.fpf.smartscan.lib.getFilesFromDir
import com.fpf.smartscan.lib.moveFile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit


class Organiser(private val context: Context) {
    var embeddingHandler: Embeddings? = null

    companion object {
        private const val TAG = "ClassificationProcessor"
    }

    private val prototypeRepository: PrototypeEmbeddingRepository = PrototypeEmbeddingRepository(
        PrototypeEmbeddingDatabase.getDatabase(context.applicationContext as Application)
            .prototypeEmbeddingDao()
    )

    init {
        embeddingHandler = Embeddings(context.resources, ModelType.IMAGE)
    }

    suspend fun processBatch(directoryUris: List<Uri>): Int {
        val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
        val imageFiles = getFilesFromDir(context, directoryUris, imageExtensions)
        if (imageFiles.isEmpty()) {
            Log.i(TAG, "No image files found for classification.")
            return 0
        }
        Log.i(TAG, "Found ${imageFiles.size} image files for classification.")

        val prototypeList: List<PrototypeEmbedding> = prototypeRepository.getAllEmbeddingsSync()
        if (prototypeList.isEmpty()) {
            Log.e(TAG, "No prototype embeddings available.")
            return 0
        }

        val semaphore = Semaphore(3)
        val results = supervisorScope {
            imageFiles.map { imageUri ->
                async {
                    semaphore.withPermit {
                        processImage(imageUri, prototypeList)
                    }
                }
            }.awaitAll()
        }

        val processedCount = results.count() { it }
        return processedCount
    }

    private suspend fun processImage(
        imageUri: Uri, prototypeEmbeddings: List<PrototypeEmbedding>
    ): Boolean {
        return try {
            val bitmap = getBitmapFromUri(context, imageUri)
            val imageEmbedding = embeddingHandler?.generateImageEmbedding(bitmap)
            bitmap.recycle()

            if (imageEmbedding == null) return false

            val similarities =
                getSimilarities(imageEmbedding, prototypeEmbeddings.map { it.embeddings })
            val bestIndex = getTopN(similarities, 1, 0.2f).firstOrNull() ?: -1
            val destinationIdentifier = prototypeEmbeddings.getOrNull(bestIndex)?.id

            if (destinationIdentifier == null) {
                Log.e(TAG, "Image classification failed for URI: $imageUri")
                false
            } else {
                moveFile(context, imageUri, destinationIdentifier.toUri())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image $imageUri: ${e.message}", e)
            false
        }
    }

    fun onComplete() {
        embeddingHandler?.closeSession()
    }
}
