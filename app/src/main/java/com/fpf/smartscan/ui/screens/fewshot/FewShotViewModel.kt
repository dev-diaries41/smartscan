package com.fpf.smartscan.ui.screens.fewshot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.R
import com.fpf.smartscan.data.fewshot.FewShotDatabase
import com.fpf.smartscan.data.fewshot.FewShotPrototypeEntity
import com.fpf.smartscan.data.fewshot.FewShotRepository
import com.fpf.smartscan.data.fewshot.FewShotSampleEntity
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.ml.models.ResourceId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel pro Few-Shot Learning UI
 *
 * Spravuje stav pro FewShotTagsScreen a poskytuje operace pro:
 * - Vytváření/editaci/mazání few-shot prototypes
 * - Přidávání/odebírání samplu
 * - Vyhledávání a filtrování prototypes
 */
class FewShotViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FewShotRepository
    private val imageEmbedder: ClipImageEmbedder

    // State pro všechny prototypes
    val allPrototypes: StateFlow<List<FewShotPrototypeEntity>>

    // State pro loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State pro error messages
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // State pro vybraný prototype (pro detail view)
    private val _selectedPrototype = MutableStateFlow<FewShotPrototypeEntity?>(null)
    val selectedPrototype: StateFlow<FewShotPrototypeEntity?> = _selectedPrototype.asStateFlow()

    // State pro samples vybraného prototype
    private val _selectedPrototypeSamples = MutableStateFlow<List<FewShotSampleEntity>>(emptyList())
    val selectedPrototypeSamples: StateFlow<List<FewShotSampleEntity>> = _selectedPrototypeSamples.asStateFlow()

    // State pro search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // State pro filtered prototypes (podle search query)
    private val _filteredPrototypes = MutableStateFlow<List<FewShotPrototypeEntity>>(emptyList())
    val filteredPrototypes: StateFlow<List<FewShotPrototypeEntity>> = _filteredPrototypes.asStateFlow()

    init {
        // Inicializace database a repository
        val database = FewShotDatabase.getDatabase(application)
        imageEmbedder = ClipImageEmbedder(
            application.resources,
            ResourceId(R.raw.image_encoder_quant_int8)
        )
        repository = FewShotRepository(
            context = application,
            prototypeDao = database.prototypeDao(),
            sampleDao = database.sampleDao(),
            imageEmbedder = imageEmbedder
        )

        // Načtení všech prototypes
        allPrototypes = repository.allPrototypes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Vytvoří nový few-shot prototype
     *
     * @param name Název prototypu (např. "Barunka")
     * @param color Barva pro UI (hex)
     * @param samples List párů (imageUri, cropRectJson)
     * @param description Volitelný popis
     * @param category Volitelná kategorie
     * @return Result s ID prototypu nebo chybou
     */
    fun createPrototype(
        name: String,
        color: Int,
        samples: List<Pair<String, String>>,
        description: String? = null,
        category: String? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Validace
                if (name.isBlank()) {
                    throw IllegalArgumentException("Název prototypu nesmí být prázdný")
                }
                if (samples.isEmpty()) {
                    throw IllegalArgumentException("Musíte vybrat alespoň jeden sample")
                }
                if (samples.size < 3) {
                    throw IllegalArgumentException("Doporučeno minimálně 3-5 samplu pro kvalitní prototype")
                }

                // Kontrola, zda prototype s tímto názvem už neexistuje
                val existing = repository.getPrototypeByName(name)
                if (existing != null) {
                    throw IllegalArgumentException("Prototype s názvem '$name' již existuje")
                }

                // Inicializace image embedder pokud není
                if (!imageEmbedder.isInitialized()) {
                    imageEmbedder.initialize()
                }

                // Vytvoření prototype
                val prototypeId = repository.createPrototype(
                    name = name,
                    color = color,
                    samples = samples,
                    description = description,
                    category = category
                )

                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Chyba při vytváření prototypu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Smaže prototype
     *
     * @param prototypeId ID prototypu ke smazání
     */
    fun deletePrototype(prototypeId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                repository.deletePrototype(prototypeId)

                // Clear selected prototype if it was deleted
                if (_selectedPrototype.value?.id == prototypeId) {
                    _selectedPrototype.value = null
                    _selectedPrototypeSamples.value = emptyList()
                }

                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Chyba při mazání prototypu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Vyb

ere prototype pro zobrazení detailu
     *
     * @param prototypeId ID prototypu
     */
    fun selectPrototype(prototypeId: Long) {
        viewModelScope.launch {
            try {
                val prototype = repository.getPrototypeById(prototypeId)
                _selectedPrototype.value = prototype

                // Load samples for selected prototype
                if (prototype != null) {
                    repository.getSamplesForPrototype(prototypeId)
                        .collect { samples ->
                            _selectedPrototypeSamples.value = samples
                        }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Chyba při načítání prototypu"
            }
        }
    }

    /**
     * Aktualizuje search query a filtruje prototypes
     *
     * @param query Search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    _filteredPrototypes.value = emptyList()
                } else {
                    val results = repository.searchPrototypes(query)
                    _filteredPrototypes.value = results
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Chyba při vyhledávání"
            }
        }
    }

    /**
     * Vymaže error message
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
