package com.fpf.smartscan.ui.screens.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.R
import com.fpf.smartscan.data.tags.MediaTagEntity
import com.fpf.smartscan.data.tags.TagDatabase
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.tags.UserTagEntity
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipTextEmbedder
import com.fpf.smartscansdk.core.ml.models.ResourceId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel pro správu user-defined tagů
 *
 * Poskytuje CRUD operace pro tagy a integraci s CLIP text embedder
 * pro generování embeddingů z textových popisů tagů.
 */
class TagViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TagRepository
    private val textEmbedder: ClipTextEmbedder

    // State pro všechny tagy
    val allTags: StateFlow<List<UserTagEntity>>

    // State pro loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State pro error messages
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // State pro tagy s počty obrázků
    private val _tagsWithCounts = MutableStateFlow<List<Pair<UserTagEntity, Int>>>(emptyList())
    val tagsWithCounts: StateFlow<List<Pair<UserTagEntity, Int>>> = _tagsWithCounts.asStateFlow()

    init {
        // Inicializace database a repository
        val database = TagDatabase.getDatabase(application)
        repository = TagRepository(
            userTagDao = database.userTagDao(),
            mediaTagDao = database.mediaTagDao()
        )

        // Inicializace CLIP text embedder
        textEmbedder = ClipTextEmbedder(
            application.resources,
            ResourceId(R.raw.text_encoder_quant_int8)
        )

        // Načtení všech tagů
        allTags = repository.allTags.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Načtení tagů s počty
        loadTagsWithCounts()
    }

    /**
     * Vytvoří nový tag
     * @param name Název tagu (např. "Rekonstrukce domu")
     * @param description Popis pro CLIP embedding (např. "fotografie renovace...")
     * @param threshold Práh pro auto-assignment (0.0-1.0)
     * @param color Barva tagu pro UI
     * @param isActive Zda je tag aktivní pro auto-tagging
     */
    suspend fun createTag(
        name: String,
        description: String,
        threshold: Float = 0.30f,
        color: Int = 0xFF2196F3.toInt(),
        isActive: Boolean = true,
        isExcluded: Boolean = false
    ): Result<Unit> {
        return try {
            _isLoading.value = true
            _error.value = null

            // Validace
            if (name.isBlank()) {
                throw IllegalArgumentException("Název tagu nesmí být prázdný")
            }
            if (description.length < 10) {
                throw IllegalArgumentException("Popis musí mít minimálně 10 znaků")
            }

            // Kontrola, zda tag s tímto názvem už neexistuje
            val existingTag = repository.getTagByName(name)
            if (existingTag != null) {
                throw IllegalArgumentException("Tag s názvem '$name' již existuje")
            }

            // Generování embeddingu z popisu
            val embedding = generateEmbedding(description)

            // Vytvoření entity
            val tag = UserTagEntity(
                name = name,
                description = description,
                embedding = embedding,
                threshold = threshold,
                color = color,
                isActive = isActive,
                isExcluded = isExcluded,
            )

            // Uložení do databáze
            repository.insertTag(tag)

            // Refresh counts
            loadTagsWithCounts()

            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Aktualizuje existující tag
     */
    suspend fun updateTag(tag: UserTagEntity): Result<Unit> {
        return try {
            _isLoading.value = true
            _error.value = null

            // Aktualizace timestamp
            val updatedTag = tag.copy(
                updatedAt = System.currentTimeMillis()
            )

            repository.updateTag(updatedTag)

            // Refresh counts
            loadTagsWithCounts()

            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Aktualizuje tag včetně embeddings (pokud se změnil popis)
     */
    suspend fun updateTagWithEmbedding(
        originalName: String,
        name: String,
        description: String,
        threshold: Float,
        color: Int,
        isActive: Boolean,
        isExcluded: Boolean
    ): Result<Unit> {
        return try {
            _isLoading.value = true
            _error.value = null

            // Validace
            if (name.isBlank()) {
                throw IllegalArgumentException("Název tagu nesmí být prázdný")
            }
            if (description.length < 10) {
                throw IllegalArgumentException("Popis musí mít minimálně 10 znaků")
            }

            // Pokud se změnil název, kontrola kolize
            if (originalName != name) {
                val existingTag = repository.getTagByName(name)
                if (existingTag != null) {
                    throw IllegalArgumentException("Tag s názvem '$name' již existuje")
                }
            }

            // Načtení původního tagu
            val originalTag = repository.getTagByName(originalName)
                ?: throw IllegalArgumentException("Tag '$originalName' nebyl nalezen")

            // Generování nového embeddingu (popis se mohl změnit)
            val embedding = generateEmbedding(description)

            // Pokud se změnil název, musíme smazat starý a vytvořit nový
            // (protože name je primary key)
            if (originalName != name) {
                repository.deleteTag(originalTag)
                val newTag = UserTagEntity(
                    name = name,
                    description = description,
                    embedding = embedding,
                    threshold = threshold,
                    color = color,
                    isActive = isActive,
                    isExcluded = isExcluded,
                    createdAt = originalTag.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
                repository.insertTag(newTag)
            } else {
                // Pouze update
                val updatedTag = originalTag.copy(
                    description = description,
                    embedding = embedding,
                    threshold = threshold,
                    color = color,
                    isExcluded = isExcluded,
                    isActive = isActive,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTag(updatedTag)
            }

            // Refresh counts
            loadTagsWithCounts()

            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Smaže tag a všechna přiřazení k obrázkům
     */
    suspend fun deleteTag(tag: UserTagEntity): Result<Unit> {
        return try {
            _isLoading.value = true
            _error.value = null

            // Smazání všech image tags s tímto názvem
            repository.deleteAllTagsWithName(tag.name)

            // Smazání user tagu
            repository.deleteTag(tag)

            // Refresh counts
            loadTagsWithCounts()

            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Získá tag s počtem přiřazených obrázků
     */
    suspend fun getTagWithImageCount(tagName: String): Pair<UserTagEntity, Int>? {
        return try {
            val tag = repository.getTagByName(tagName) ?: return null
            val count = repository.getImageCountForTag(tagName)
            tag to count
        } catch (e: Exception) {
            _error.value = e.message
            null
        }
    }

    /**
     * Načte všechny tagy s počty obrázků
     */
    private fun loadTagsWithCounts() {
        viewModelScope.launch {
            try {
                repository.allTags.collect { tags ->
                    _isLoading.value = true
                    val withCounts = tags.map { tag ->
                        val count = repository.getImageCountForTag(tag.name)
                        tag to count
                    }
                    _tagsWithCounts.value = withCounts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    /**
     * Získá tagy pro konkrétní obrázek
     */
    fun getTagsForMedia(imageId: Long): StateFlow<List<MediaTagEntity>> {
        val flow = MutableStateFlow<List<MediaTagEntity>>(emptyList())
        viewModelScope.launch {
            repository.getTagsForMediaFlow(imageId).collect { tags ->
                flow.value = tags
            }
        }
        return flow.asStateFlow()
    }

    /**
     * Ručně přidá tag k obrázku
     */
    suspend fun addTagToMedia(imageId: Long, tagName: String): Result<Unit> {
        return try {
            _error.value = null

            val tag = repository.getTagByName(tagName)
                ?: throw IllegalArgumentException("Tag '$tagName' nebyl nalezen")

            val imageTag = MediaTagEntity(
                mediaId = imageId,
                tagName = tagName,
                confidence = 1.0f, // Manual assignment = 100% confidence
                isUserAssigned = true
            )

            repository.insertMediaTag(imageTag)

            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    /**
     * Odebere tag z obrázku
     */
    suspend fun removeTagFromMedia(imageId: Long, tagName: String): Result<Unit> {
        return try {
            _error.value = null

            repository.deleteSpecificTag(imageId, tagName)

            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        }
    }

    /**
     * Vyčistí error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Importuje preset tagy do databáze
     *
     * Tato metoda:
     * - Načte preset tagy z PresetTags.RECOMMENDED
     * - Vygeneruje embeddingy pro každý preset tag
     * - Vloží pouze tagy, které ještě neexistují (skip duplicit)
     * - Vrátí počet nově importovaných tagů
     *
     * @param onProgress Callback pro progress reporting (current, total)
     * @return Result s počtem importovaných tagů nebo chybou
     */
    suspend fun importPresetTags(
        onProgress: ((Int, Int) -> Unit)? = null
    ): Result<Int> {
        return try {
            _isLoading.value = true
            _error.value = null

            val presetTags = com.fpf.smartscan.data.tags.PresetTags.RECOMMENDED
            val total = presetTags.size
            var importedCount = 0

            presetTags.forEachIndexed { index, preset ->
                try {
                    // Kontrola, zda tag již existuje
                    val existingTag = repository.getTagByName(preset.name)
                    if (existingTag == null) {
                        // Generování embeddingu
                        val embedding = generateEmbedding(preset.description)

                        // Vytvoření entity
                        val tag = UserTagEntity(
                            name = preset.name,
                            description = preset.description,
                            embedding = embedding,
                            threshold = preset.threshold,
                            color = preset.color,
                            isActive = true
                        )

                        // Vložení do databáze
                        repository.insertTag(tag)
                        importedCount++
                    }

                    onProgress?.invoke(index + 1, total)
                } catch (e: Exception) {
                    // Pokračujeme i když jeden tag selže
                    _error.value = "Chyba při importu tagu ${preset.name}: ${e.message}"
                }
            }

            // Refresh counts
            loadTagsWithCounts()

            Result.success(importedCount)
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Generuje CLIP embedding z textového popisu
     */
    private suspend fun generateEmbedding(description: String): FloatArray {
        if (!textEmbedder.isInitialized()) {
            textEmbedder.initialize()
        }
        return textEmbedder.embed(description)
    }
}
