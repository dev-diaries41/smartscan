package com.fpf.smartscan.services

import android.content.Context
import android.util.Log
import com.fpf.smartscan.data.tags.ImageTagEntity
import com.fpf.smartscan.data.tags.TagDatabase
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.tags.UserTagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Service pro automatické přiřazování tagů na základě CLIP embeddingů
 *
 * Používá cosine similarity pro porovnání image embeddingů s tag embeddingy.
 * Pokud similarity překročí threshold definovaný v tagu, tag se automaticky přiřadí.
 */
class TaggingService(context: Context) {

    private val repository: TagRepository

    init {
        val database = TagDatabase.getDatabase(context)
        repository = TagRepository(
            userTagDao = database.userTagDao(),
            imageTagDao = database.imageTagDao()
        )
    }

    companion object {
        private const val TAG = "TaggingService"
    }

    /**
     * Přiřadí tagy jednomu obrázku na základě jeho embeddingu
     *
     * @param imageId MediaStore ID obrázku
     * @param imageEmbedding CLIP embedding obrázku (512 floatů)
     * @return List přiřazených tagů
     */
    suspend fun assignTags(
        imageId: Long,
        imageEmbedding: FloatArray
    ): List<ImageTagEntity> = withContext(Dispatchers.IO) {
        try {
            // Načtení aktivních tagů
            val activeTags = repository.getActiveTagsSync()

            if (activeTags.isEmpty()) {
                Log.d(TAG, "No active tags found for auto-tagging")
                return@withContext emptyList()
            }

            // Smazání existujících auto-assigned tagů pro tento obrázek
            // (ponecháme pouze user-assigned)
            // Používáme DELETE query místo entity delete pro spolehlivost
            repository.deleteAutoAssignedTagsForImage(imageId)

            // Přiřazení nových tagů
            val assignedTags = mutableListOf<ImageTagEntity>()

            activeTags.forEach { userTag ->
                val similarity = cosineSimilarity(imageEmbedding, userTag.embedding)

                if (similarity >= userTag.threshold) {
                    val imageTag = ImageTagEntity(
                        imageId = imageId,
                        tagName = userTag.name,
                        confidence = similarity,
                        isUserAssigned = false
                    )
                    assignedTags.add(imageTag)
                }
            }

            // Batch insert do databáze
            if (assignedTags.isNotEmpty()) {
                repository.insertImageTags(assignedTags)
                Log.d(TAG, "Assigned ${assignedTags.size} tags to image $imageId")
            }

            assignedTags
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning tags to image $imageId", e)
            emptyList()
        }
    }

    /**
     * Batch přiřazení tagů pro více obrázků
     *
     * @param images List párů (imageId, embedding)
     * @param onProgress Callback pro progress reporting (current, total)
     * @return Celkový počet přiřazených tagů
     */
    suspend fun assignTagsBatch(
        images: List<Pair<Long, FloatArray>>,
        onProgress: ((Int, Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        try {
            var totalAssigned = 0
            val total = images.size

            images.forEachIndexed { index, (imageId, embedding) ->
                val assigned = assignTags(imageId, embedding)
                totalAssigned += assigned.size

                onProgress?.invoke(index + 1, total)
            }

            Log.d(TAG, "Batch tagging complete: $totalAssigned tags assigned to $total images")
            totalAssigned
        } catch (e: Exception) {
            Log.e(TAG, "Error in batch tagging", e)
            0
        }
    }

    /**
     * Re-tag všech obrázků s existujícími embeddingy
     *
     * Používá FileEmbeddingStore pro načtení embeddingů.
     * Užitečné po vytvoření nového tagu nebo změně thresholdů.
     *
     * @param embeddings List embeddingů z FileEmbeddingStore
     * @param onProgress Callback pro progress reporting
     */
    suspend fun retagAllImages(
        embeddings: List<com.fpf.smartscansdk.core.ml.embeddings.Embedding>,
        onProgress: ((Int, Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        try {
            val imagePairs = embeddings.map { embedding ->
                embedding.id to embedding.embeddings
            }

            assignTagsBatch(imagePairs, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Error in retag all images", e)
            0
        }
    }

    /**
     * Vypočítá cosine similarity mezi dvěma vektory
     *
     * Cosine similarity měří úhel mezi vektory v n-dimenzionálním prostoru.
     * Vrací hodnotu 0.0-1.0, kde 1.0 = identické vektory.
     *
     * @param a První vektor (image embedding)
     * @param b Druhý vektor (tag embedding)
     * @return Similarity score (0.0-1.0)
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) {
            "Vectors must have same length (a=${a.size}, b=${b.size})"
        }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)

        return if (denominator > 0f) {
            dotProduct / denominator
        } else {
            0f
        }
    }

    /**
     * Získá statistiky auto-taggingu
     *
     * @return Pair(celkový počet tagged obrázků, celkový počet přiřazených tagů)
     */
    suspend fun getTaggingStats(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val allTags = repository.getActiveTagsSync()
            var totalImages = 0
            var totalAssignments = 0

            allTags.forEach { tag ->
                val count = repository.getImageCountForTag(tag.name)
                if (count > 0) {
                    totalImages += count
                    totalAssignments += count
                }
            }

            totalImages to totalAssignments
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tagging stats", e)
            0 to 0
        }
    }
}
