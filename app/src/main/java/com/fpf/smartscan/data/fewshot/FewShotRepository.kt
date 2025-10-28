package com.fpf.smartscan.data.fewshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository pro Few-Shot Learning
 *
 * Poskytuje high-level operace pro práci s few-shot prototypes:
 * - Vytváření prototypes z múltiple samplu
 * - Přidávání/odebírání samplu
 * - Re-compute průměrného embeddingu
 * - Kombinace embeddingů pro vyhledávání
 *
 * Používá ClipImageEmbedder pro extrakci embeddingů z oříznutých obrázků.
 */
class FewShotRepository(
    private val context: Context,
    private val prototypeDao: FewShotPrototypeDao,
    private val sampleDao: FewShotSampleDao,
    private val imageEmbedder: ClipImageEmbedder
) {
    companion object {
        private const val TAG = "FewShotRepository"
    }

    // Flow properties
    val allPrototypes: Flow<List<FewShotPrototypeEntity>> = prototypeDao.getAllPrototypes()

    /**
     * Vytvoří nový few-shot prototype z multiple samplu
     *
     * @param name Název prototypu (např. "Barunka", "Jonasek")
     * @param color Barva pro UI (hex)
     * @param samples List párů (imageUri, cropRectJson)
     * @param description Volitelný popis
     * @param category Volitelná kategorie ("person", "object", "scene", "style")
     * @return ID vytvořeného prototypu
     * @throws IllegalArgumentException pokud samples je prázdný
     */
    suspend fun createPrototype(
        name: String,
        color: Int,
        samples: List<Pair<String, String>>, // (imageUri, cropRectJson)
        description: String? = null,
        category: String? = null
    ): Long = withContext(Dispatchers.IO) {
        require(samples.isNotEmpty()) { "Cannot create prototype without samples" }

        Log.i(TAG, "Creating prototype '$name' with ${samples.size} samples")

        // 1. Extract embeddings from all samples
        val embeddings = samples.mapIndexed { index, (uri, cropRect) ->
            Log.d(TAG, "Extracting embedding from sample ${index + 1}/${samples.size}")
            extractEmbeddingFromCrop(uri, cropRect)
        }

        // 2. Compute average embedding
        val avgEmbedding = computeAverageEmbedding(embeddings)
        Log.i(TAG, "Computed average embedding for '$name'")

        // 3. Create prototype
        val prototype = FewShotPrototypeEntity(
            name = name,
            embedding = avgEmbedding,
            color = color,
            sampleCount = samples.size,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            description = description,
            category = category
        )
        val prototypeId = prototypeDao.insertPrototype(prototype)
        Log.i(TAG, "Inserted prototype with ID: $prototypeId")

        // 4. Save individual samples
        val sampleEntities = samples.mapIndexed { index, (uri, cropRect) ->
            FewShotSampleEntity(
                prototypeId = prototypeId,
                imageUri = uri,
                cropRect = cropRect,
                embedding = embeddings[index],
                addedAt = System.currentTimeMillis()
            )
        }
        sampleDao.insertSamples(sampleEntities)
        Log.i(TAG, "Inserted ${sampleEntities.size} samples for prototype ID: $prototypeId")

        prototypeId
    }

    /**
     * Přidá nový sample do existujícího prototypu
     * Automaticky re-compute průměrný embedding
     *
     * @param prototypeId ID prototypu
     * @param imageUri URI obrázku
     * @param cropRectJson JSON reprezentace crop oblasti
     */
    suspend fun addSampleToPrototype(
        prototypeId: Long,
        imageUri: String,
        cropRectJson: String
    ) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Adding sample to prototype ID: $prototypeId")

        // 1. Extract embedding from new sample
        val embedding = extractEmbeddingFromCrop(imageUri, cropRectJson)

        // 2. Add sample to DB
        val sample = FewShotSampleEntity(
            prototypeId = prototypeId,
            imageUri = imageUri,
            cropRect = cropRectJson,
            embedding = embedding,
            addedAt = System.currentTimeMillis()
        )
        sampleDao.insertSample(sample)
        Log.d(TAG, "Sample added to DB")

        // 3. Re-compute prototype average
        recomputePrototype(prototypeId)
    }

    /**
     * Odebere sample z prototypu
     * Automaticky re-compute průměrný embedding
     * Pokud je to poslední sample, smaže celý prototype
     *
     * @param sampleId ID samplu k odebrání
     * @param prototypeId ID prototypu
     */
    suspend fun removeSampleFromPrototype(
        sampleId: Long,
        prototypeId: Long
    ) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Removing sample ID: $sampleId from prototype ID: $prototypeId")

        sampleDao.deleteSampleById(sampleId)
        Log.d(TAG, "Sample deleted from DB")

        recomputePrototype(prototypeId)
    }

    /**
     * Re-compute průměrný embedding prototypu z aktuálních samplu
     * Pokud prototype nemá žádné samples, smaže jej
     *
     * @param prototypeId ID prototypu k re-compute
     */
    private suspend fun recomputePrototype(prototypeId: Long) {
        Log.i(TAG, "Re-computing prototype ID: $prototypeId")

        val prototype = prototypeDao.getPrototypeById(prototypeId)
        if (prototype == null) {
            Log.w(TAG, "Prototype ID $prototypeId not found")
            return
        }

        val samples = sampleDao.getSamplesForPrototypeSync(prototypeId)

        if (samples.isEmpty()) {
            // Pokud nejsou žádné samples, smazat prototype
            Log.w(TAG, "No samples found for prototype ID $prototypeId, deleting prototype")
            prototypeDao.deletePrototypeById(prototypeId)
            return
        }

        // Compute new average
        val avgEmbedding = computeAverageEmbedding(samples.map { it.embedding })

        val updatedPrototype = prototype.copy(
            embedding = avgEmbedding,
            sampleCount = samples.size,
            updatedAt = System.currentTimeMillis()
        )
        prototypeDao.updatePrototype(updatedPrototype)

        Log.i(TAG, "Prototype ID $prototypeId re-computed with ${samples.size} samples")
    }

    /**
     * Získá prototype podle ID
     *
     * @param id ID prototypu
     * @return Prototype nebo null
     */
    suspend fun getPrototypeById(id: Long): FewShotPrototypeEntity? {
        return prototypeDao.getPrototypeById(id)
    }

    /**
     * Získá prototype podle jména
     *
     * @param name Název prototypu
     * @return Prototype nebo null
     */
    suspend fun getPrototypeByName(name: String): FewShotPrototypeEntity? {
        return prototypeDao.getPrototypeByName(name)
    }

    /**
     * Získá samples pro daný prototype
     *
     * @param prototypeId ID prototypu
     * @return Flow s listem samples
     */
    fun getSamplesForPrototype(prototypeId: Long): Flow<List<FewShotSampleEntity>> {
        return sampleDao.getSamplesForPrototype(prototypeId)
    }

    /**
     * Vyhledá prototypes podle query
     *
     * @param query Search query
     * @return List prototypes které matchují query
     */
    suspend fun searchPrototypes(query: String): List<FewShotPrototypeEntity> {
        return prototypeDao.searchPrototypes(query)
    }

    /**
     * Aktualizuje metadata prototypu (bez změny embeddingu)
     *
     * @param prototypeId ID prototypu
     * @param name Nový název
     * @param color Nová barva
     * @param description Nový popis
     * @param category Nová kategorie
     */
    suspend fun updatePrototypeMetadata(
        prototypeId: Long,
        name: String,
        color: Int,
        description: String?,
        category: String?
    ) = withContext(Dispatchers.IO) {
        val prototype = prototypeDao.getPrototypeById(prototypeId) ?: return@withContext

        val updated = prototype.copy(
            name = name,
            color = color,
            description = description,
            category = category,
            updatedAt = System.currentTimeMillis()
        )
        prototypeDao.updatePrototype(updated)

        Log.i(TAG, "Updated metadata for prototype ID: $prototypeId")
    }

    /**
     * Smaže prototype
     * Cascade delete automaticky smaže všechny samples
     *
     * @param prototypeId ID prototypu ke smazání
     */
    suspend fun deletePrototype(prototypeId: Long) {
        Log.i(TAG, "Deleting prototype ID: $prototypeId")
        prototypeDao.deletePrototypeById(prototypeId)
        // Samples se smažou automaticky (CASCADE)
    }

    /**
     * Získá počet všech prototypes
     *
     * @return Počet prototypes
     */
    suspend fun getPrototypeCount(): Int {
        return prototypeDao.getPrototypeCount()
    }

    /**
     * Extrahuje embedding z oříznutého obrázku
     *
     * @param imageUri URI obrázku
     * @param cropRectJson JSON reprezentace crop oblasti
     * @return Embedding (768 dim)
     */
    private suspend fun extractEmbeddingFromCrop(
        imageUri: String,
        cropRectJson: String
    ): FloatArray = withContext(Dispatchers.IO) {
        // Parse crop rect
        val cropRect = CropRect.fromJson(cropRectJson)

        // Load and crop image
        val bitmap = loadAndCropImage(imageUri, cropRect)

        // Extract embedding
        val embedding = imageEmbedder.embed(bitmap)

        // Clean up bitmap
        bitmap.recycle()

        embedding
    }

    /**
     * Načte obrázek a ořízne jej podle crop oblasti
     *
     * @param imageUri URI obrázku
     * @param cropRect Crop oblast
     * @return Oříznutý bitmap
     */
    private suspend fun loadAndCropImage(
        imageUri: String,
        cropRect: CropRect
    ): Bitmap = withContext(Dispatchers.IO) {
        val uri = Uri.parse(imageUri)
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image URI: $imageUri")

        val fullBitmap = inputStream.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalArgumentException("Cannot decode image from URI: $imageUri")

        // Crop bitmap
        val croppedBitmap = try {
            Bitmap.createBitmap(
                fullBitmap,
                cropRect.left.coerceIn(0, fullBitmap.width),
                cropRect.top.coerceIn(0, fullBitmap.height),
                cropRect.width.coerceAtMost(fullBitmap.width - cropRect.left),
                cropRect.height.coerceAtMost(fullBitmap.height - cropRect.top)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error cropping bitmap", e)
            throw e
        }

        // Clean up full bitmap
        if (croppedBitmap !== fullBitmap) {
            fullBitmap.recycle()
        }

        croppedBitmap
    }

    /**
     * Spočítá průměrný embedding z multiple embeddingů
     *
     * @param embeddings List embeddingů k průměrování
     * @return Průměrný embedding
     * @throws IllegalArgumentException pokud embeddings je prázdný
     */
    fun computeAverageEmbedding(embeddings: List<FloatArray>): FloatArray {
        require(embeddings.isNotEmpty()) { "Cannot compute average of empty embeddings list" }

        val dimension = embeddings[0].size
        val sum = FloatArray(dimension) { 0f }

        embeddings.forEach { embedding ->
            require(embedding.size == dimension) { "All embeddings must have same dimension" }
            for (i in embedding.indices) {
                sum[i] += embedding[i]
            }
        }

        return sum.map { it / embeddings.size }.toFloatArray()
    }
}
