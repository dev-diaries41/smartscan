package com.fpf.smartscan.services

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.fpf.smartscan.R
import com.fpf.smartscan.data.tags.PresetTags
import com.fpf.smartscan.data.tags.TagDatabase
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.tags.UserTagEntity
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipTextEmbedder
import com.fpf.smartscansdk.core.ml.models.ResourceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service pro jednorázovou inicializaci preset tagů při prvním spuštění aplikace.
 *
 * Tento service:
 * - Zkontroluje, zda již byly preset tagy inicializovány
 * - Vygeneruje CLIP embeddingy pro každý preset tag
 * - Vloží všechny preset tagy do databáze
 * - Označí inicializaci jako dokončenou
 *
 * Inicializace probíhá v background thread a je thread-safe.
 */
class TagInitializer(
    private val context: Context,
    private val resources: Resources
) {

    private val repository: TagRepository
    private val textEmbedder: ClipTextEmbedder

    companion object {
        private const val TAG = "TagInitializer"
        private const val PREFS_NAME = "tag_initializer_prefs"
        private const val KEY_PRESET_TAGS_INITIALIZED = "preset_tags_initialized"

        /**
         * Kontrola, zda již byly preset tagy inicializovány
         */
        fun isInitialized(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_PRESET_TAGS_INITIALIZED, false)
        }

        /**
         * Označí preset tagy jako inicializované
         */
        private fun markAsInitialized(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_PRESET_TAGS_INITIALIZED, true).apply()
        }

        /**
         * Reset inicializace (pro debugging nebo re-import)
         */
        fun resetInitialization(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_PRESET_TAGS_INITIALIZED, false).apply()
        }
    }

    init {
        val database = TagDatabase.getDatabase(context)
        repository = TagRepository(
            userTagDao = database.userTagDao(),
            imageTagDao = database.imageTagDao()
        )

        textEmbedder = ClipTextEmbedder(
            resources,
            ResourceId(R.raw.text_encoder_quant_int8)
        )
    }

    /**
     * Inicializuje preset tagy do databáze
     *
     * @param onProgress Callback pro progress reporting (current, total)
     * @return Result s počtem úspěšně inicializovaných tagů nebo chybou
     */
    suspend fun initializePresetTags(
        onProgress: ((Int, Int) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Double-check - pokud již jsou inicializovány, skip
            if (isInitialized(context)) {
                Log.d(TAG, "Preset tags already initialized, skipping")
                return@withContext Result.success(0)
            }

            // Kontrola, zda v databázi již nejsou nějaké tagy
            val existingTagCount = repository.getTagCount()
            if (existingTagCount > 0) {
                Log.d(TAG, "Database already contains $existingTagCount tags, skipping preset initialization")
                markAsInitialized(context)
                return@withContext Result.success(0)
            }

            Log.d(TAG, "Starting preset tags initialization...")

            // Inicializace CLIP text embedder
            if (!textEmbedder.isInitialized()) {
                textEmbedder.initialize()
            }

            val presetTags = PresetTags.RECOMMENDED
            val total = presetTags.size
            var successCount = 0

            presetTags.forEachIndexed { index, preset ->
                try {
                    // Generování embeddingu
                    val embedding = textEmbedder.embed(preset.description)

                    // Vytvoření entity
                    val tag = UserTagEntity(
                        name = preset.name,
                        description = preset.description,
                        embedding = embedding,
                        threshold = preset.threshold,
                        color = preset.color,
                        isActive = true // Všechny preset tagy jsou defaultně aktivní
                    )

                    // Vložení do databáze
                    repository.insertTag(tag)
                    successCount++

                    Log.d(TAG, "Initialized tag: ${preset.name} (${index + 1}/$total)")

                    // Progress callback
                    onProgress?.invoke(index + 1, total)

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize tag: ${preset.name}", e)
                }
            }

            // Označit jako dokončeno
            markAsInitialized(context)

            Log.d(TAG, "Preset tags initialization complete: $successCount/$total tags initialized")

            Result.success(successCount)

        } catch (e: Exception) {
            Log.e(TAG, "Error during preset tags initialization", e)
            Result.failure(e)
        }
    }

    /**
     * Asynchronní inicializace bez progress callbacku
     * Vhodné pro background inicializaci při startu aplikace
     */
    suspend fun initializePresetTagsAsync(): Result<Int> {
        return initializePresetTags(onProgress = null)
    }
}
