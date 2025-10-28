package com.fpf.smartscan.ui.screens.search

import android.app.Application
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.images.ImageEmbeddingDatabase
import com.fpf.smartscan.data.images.ImageEmbeddingRepository
import com.fpf.smartscan.lib.getImageUriFromId
import com.fpf.smartscan.lib.ShareManager
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.data.QueryType
import com.fpf.smartscan.data.tags.TagDatabase
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.tags.UserTagEntity
import com.fpf.smartscan.data.videos.VideoEmbeddingDatabase
import com.fpf.smartscan.data.fewshot.FewShotDatabase
import com.fpf.smartscan.data.fewshot.FewShotPrototypeEntity
import com.fpf.smartscan.data.videos.VideoEmbeddingRepository
import com.fpf.smartscan.lib.canOpenUri
import com.fpf.smartscan.lib.deleteFiles
import com.fpf.smartscan.lib.getVideoUriFromId
import com.fpf.smartscan.lib.ImageIndexListener
import com.fpf.smartscan.lib.moveFiles
import com.fpf.smartscan.lib.VideoIndexListener
import com.fpf.smartscan.services.TranslationService
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.CLIP_EMBEDDING_LENGTH
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipTextEmbedder
import com.fpf.smartscansdk.core.ml.models.ResourceId
import com.fpf.smartscansdk.core.utils.getBitmapFromUri
import com.fpf.smartscansdk.extensions.embeddings.FileEmbeddingRetriever
import com.fpf.smartscansdk.extensions.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.extensions.indexers.ImageIndexer
import com.fpf.smartscansdk.extensions.indexers.VideoIndexer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicBoolean

class SearchViewModel(private val application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SearchViewModel"
        const val RESULTS_BATCH_SIZE = 30
    }

    val imageIndexProgress = ImageIndexListener.progress
    val imageIndexStatus = ImageIndexListener.indexingStatus
    val videoIndexProgress = VideoIndexListener.progress
    val videoIndexStatus = VideoIndexListener.indexingStatus

    val imageStore = FileEmbeddingStore(application.filesDir, ImageIndexer.INDEX_FILENAME, CLIP_EMBEDDING_LENGTH)
    val imageRetriever = FileEmbeddingRetriever(imageStore)

    val videoStore = FileEmbeddingStore(application.filesDir,  VideoIndexer.INDEX_FILENAME, CLIP_EMBEDDING_LENGTH )
    val videoRetriever = FileEmbeddingRetriever(videoStore)

    private val textEmbedder = ClipTextEmbedder(application.resources, ResourceId(R.raw.text_encoder_quant_int8))
    private val imageEmbedder = ClipImageEmbedder(application.resources, ResourceId(R.raw.image_encoder_quant_int8))
    private val translationService = TranslationService()

    private val repository: ImageEmbeddingRepository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )
    private val videoRepository: VideoEmbeddingRepository = VideoEmbeddingRepository(
        VideoEmbeddingDatabase.getDatabase(application).videoEmbeddingDao()
    )

    private val tagRepository: TagRepository = TagRepository(
        userTagDao = TagDatabase.getDatabase(application).userTagDao(),
        mediaTagDao = TagDatabase.getDatabase(application).mediaTagDao()
    )

    private val fewShotDatabase = FewShotDatabase.getDatabase(application)
    private val fewShotPrototypeDao = fewShotDatabase.prototypeDao()

    private val _hasRefreshedImageIndex = MutableStateFlow(false)
    private val _hasRefreshedVideoIndex = MutableStateFlow(false)
    private val _hasShownImageIndexAlert = MutableStateFlow(false)
    private val _hasShownVideoIndexAlert = MutableStateFlow(false)
    private val _isVideoIndexAlertVisible = MutableStateFlow(false)
    val isVideoIndexAlertVisible: StateFlow<Boolean> = _isVideoIndexAlertVisible
    private val _isImageIndexAlertVisible = MutableStateFlow(false)
    val isImageIndexAlertVisible: StateFlow<Boolean> = _isImageIndexAlertVisible

    private val _mediaType = MutableStateFlow(MediaType.IMAGE)
    val mediaType: StateFlow<MediaType> = _mediaType

    private val hasAnyImages: Flow<Boolean> = repository.hasAnyEmbedding
    private val hasAnyVideos: Flow<Boolean> = videoRepository.hasAnyVideoEmbeddings
    val hasIndexed: StateFlow<Boolean?> =
        combine(_mediaType, hasAnyImages, hasAnyVideos) { mode, anyImages, anyVideos ->
            when (mode) {
                MediaType.IMAGE -> anyImages || imageStore.exists
                MediaType.VIDEO -> anyVideos || videoStore.exists
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // Přeložený dotaz (pro zobrazení v UI)
    private val _translatedQuery = MutableStateFlow<String?>(null)
    val translatedQuery: StateFlow<String?> = _translatedQuery

    private val _searchResults = MutableStateFlow<List<Uri>>(emptyList())
    val searchResults: StateFlow<List<Uri>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val isLoadingMoreSearchResults = AtomicBoolean(false)
    private val _totalResults = MutableStateFlow(0)
    val totalResults: StateFlow<Int> = _totalResults

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _canSearchImages = MutableStateFlow(false)
    private val _canSearchVideos = MutableStateFlow(false)
    val canSearch: StateFlow<Boolean> =
        combine(_mediaType, _canSearchImages, _canSearchVideos) { mode, canImages, canVideos ->
            when (mode) {
                MediaType.IMAGE -> canImages
                MediaType.VIDEO -> canVideos
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _resultToView = MutableStateFlow<Uri?>(null)
    val resultToView: StateFlow<Uri?> = _resultToView

    // Index pro viewer s swipe navigací
    private val _viewerIndex = MutableStateFlow<Int?>(null)
    val viewerIndex: StateFlow<Int?> = _viewerIndex

    private val _queryType = MutableStateFlow(QueryType.TEXT)
    val queryType: StateFlow<QueryType> = _queryType

    private val _searchImageUri = MutableStateFlow<Uri?>(null)
    val searchImageUri: StateFlow<Uri?> = _searchImageUri

    // Crop functionality - uložený oříznutý bitmap pro vyhledávání
    private val _croppedBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val croppedBitmap: StateFlow<android.graphics.Bitmap?> = _croppedBitmap

    // Flag zda zobrazit crop dialog
    private val _showCropDialog = MutableStateFlow(false)
    val showCropDialog: StateFlow<Boolean> = _showCropDialog

    // Selection state pro multi-select mode
    private val _selectedUris = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedUris: StateFlow<Set<Uri>> = _selectedUris

    // Tag filtering state
    private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagFilters: StateFlow<Set<String>> = _selectedTagFilters

    private val _availableTagsWithCounts = MutableStateFlow<List<Pair<UserTagEntity, Int>>>(emptyList())
    val availableTagsWithCounts: StateFlow<List<Pair<UserTagEntity, Int>>> = _availableTagsWithCounts

    // Date range filtering state
    private val _dateRangeStart = MutableStateFlow<Long?>(null)
    val dateRangeStart: StateFlow<Long?> = _dateRangeStart

    private val _dateRangeEnd = MutableStateFlow<Long?>(null)
    val dateRangeEnd: StateFlow<Long?> = _dateRangeEnd

    // Všechny výsledky před aplikací tag filtru
    private val _unfilteredSearchResults = MutableStateFlow<List<Uri>>(emptyList())

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    // Few-Shot Learning state
    private val _selectedFewShotPrototype = MutableStateFlow<FewShotPrototypeEntity?>(null)
    val selectedFewShotPrototype: StateFlow<FewShotPrototypeEntity?> = _selectedFewShotPrototype

    private val _availableFewShotPrototypes = MutableStateFlow<List<FewShotPrototypeEntity>>(emptyList())
    val availableFewShotPrototypes: StateFlow<List<FewShotPrototypeEntity>> = _availableFewShotPrototypes

    var imageEmbedderLastUsage: Long? = null
    var textEmbedderLastUsage: Long? = null
    val modelShutdownThreshold: Long = 20_000L

    init {
        loadImageIndex()
    }

    private fun loadImageIndex(){
        loadIndex(imageStore, { repository.getAllEmbeddingsWithFileSync() }, _canSearchImages)
    }

    private fun loadVideoIndex(){
        loadIndex(videoStore, { videoRepository.getAllEmbeddingsWithFileSync() }, _canSearchVideos)
    }

    private fun loadIndex(
        store: FileEmbeddingStore,
        fetchFromRoom: suspend () -> List<Embedding>,
        canSearchEmitter: MutableStateFlow<Boolean>
    ){
        viewModelScope.launch(Dispatchers.IO){
            try {
                _isLoading.emit(true)

                val embeddings = if(store.exists) {
                    store.get()
                } else  {
                    // For backwards compatibility with old Room storage
                    val embs = fetchFromRoom()
                    store.add(embs)
                    embs
                }
                if(embeddings.isNotEmpty()){
                    canSearchEmitter.emit(true)
                }
            }catch (e: Exception){
                _error.emit(application.getString(R.string.search_error_index_loading))
                Log.e(TAG, "Error loading index: $e")
            }finally {
                _isLoading.emit(false)
            }
        }
    }

    fun refreshIndex(mode : MediaType){
        if(mode == MediaType.IMAGE && !_hasRefreshedImageIndex.value){
            loadImageIndex()
            _hasRefreshedImageIndex.value = true
        }else if (mode == MediaType.VIDEO && !_hasRefreshedVideoIndex.value){
            loadVideoIndex()
            _hasRefreshedVideoIndex.value = true
        }
    }

    fun setQuery(newQuery: String) {
        _query.value = newQuery
        // Vymazat přeložený dotaz při změně textu
        _translatedQuery.value = null
        if(newQuery.isEmpty()){
            _error.value = null
        }
    }

    fun clearResults(){
        _searchResults.value = emptyList()
        _unfilteredSearchResults.value = emptyList()
        _totalResults.value = 0
    }

    fun setMediaType(type: MediaType) {
        _mediaType.value = type
        reset()

        // saves memory by lazy loading video index
        // This check is only valid if useCache true, which is default
        if(type == MediaType.VIDEO && !videoStore.isCached){
            viewModelScope.launch(Dispatchers.IO){loadVideoIndex()}
        }
    }

    private fun reset(){
        _error.value = null
        _query.value = ""
        _unfilteredSearchResults.value = emptyList()
        clearResults()
    }


    fun textSearch(threshold: Float = 0.2f) {
        val currentQuery = _query.value
        if (currentQuery.isBlank()) {
            _error.value = application.getString(R.string.search_error_empty_query)
            return
        }

        val store = if(_mediaType.value == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                // FÁZE 1: Překlad textu a zobrazení v UI
                // ========================================

                // 1. Inicializace translátoru (lazy - pouze první použití)
                if (!translationService.isInitialized) {
                    translationService.initialize()
                }

                // 2. Automatická detekce jazyka a překlad CS→EN
                var detectedLanguage = "en"
                val translatedQuery = translationService.translateToEnglish(currentQuery,
                    onLanguageDetected = { language: String, confidence: Float ->
                        detectedLanguage = language
                        Log.d(TAG, "Detekován jazyk: $language (${confidence * 100}% confidence)")
                    }
                )

                Log.i(TAG, "Původní dotaz: \"$currentQuery\"")
                Log.i(TAG, "Přeložený dotaz: \"$translatedQuery\"")

                // 3. Nastavit přeložený text pro UI (pokud se text změnil)
                // Zobrazíme překlad i když jazyk není rozpoznán (und), protože ML Kit
                // má problémy s detekcí krátkých slov
                if (translatedQuery != currentQuery) {
                    Log.i(TAG, "✅ Zobrazuji překlad v UI: \"$currentQuery\" → \"$translatedQuery\" (jazyk: $detectedLanguage)")
                    _translatedQuery.emit(translatedQuery)
                } else {
                    Log.i(TAG, "ℹ️ Překlad se nezobrazuje - text je stejný (jazyk: $detectedLanguage)")
                    _translatedQuery.emit(null)
                }

                // 4. PAUZA - dej UI čas zobrazit překlad (500ms)
                Log.d(TAG, "⏸️ Pauza 500ms pro zobrazení překladu...")
                kotlinx.coroutines.delay(500)
                Log.d(TAG, "▶️ Pokračuji s embedding generation...")

                // FÁZE 2: Vektorové vyhledávání
                // ========================================

                // 5. Generování embeddings z přeloženého textu
                if(!textEmbedder.isInitialized()){
                    textEmbedder.initialize()
                }
                if(shouldShutdownModel(imageEmbedderLastUsage)) imageEmbedder.closeSession() // prevent keeping both models open
                var embedding = textEmbedder.embed(translatedQuery)

                // 6. Few-Shot kombinace (pokud je vybraný prototype)
                val fewShotPrototype = _selectedFewShotPrototype.value
                if (fewShotPrototype != null) {
                    Log.i(TAG, "Combining text embedding with few-shot prototype: ${fewShotPrototype.name}")
                    embedding = combineEmbeddings(
                        baseEmbedding = embedding,
                        fewShotEmbedding = fewShotPrototype.embedding,
                        fewShotWeight = 0.5f // 50% text, 50% few-shot
                    )
                    Log.i(TAG, "Combined embedding created")
                }

                // 7. Vyhledávání
                search(store, embedding, threshold)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _error.emit(application.getString(R.string.search_error_unknown))
            } finally {
                _isLoading.emit(false)
                textEmbedderLastUsage = System.currentTimeMillis()
            }
        }
    }

    fun imageSearch(threshold: Float = 0.2f) {
        if (_searchImageUri.value == null) {
            Log.w(TAG, "imageSearch: searchImageUri is null")
            return
        }

        val store = if(_mediaType.value == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }
        _isLoading.value = true
        _error.value = null

        Log.i(TAG, "imageSearch: Starting search. Cropped bitmap exists: ${_croppedBitmap.value != null}")

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!imageEmbedder.isInitialized()){
                    imageEmbedder.initialize()
                }
                if(shouldShutdownModel(textEmbedderLastUsage)) textEmbedder.closeSession() // prevent keeping both models open

                // Pokud existuje cropped bitmap, použij ten místo celého obrázku
                val bitmap = _croppedBitmap.value ?: getBitmapFromUri(application, _searchImageUri.value!!, ClipConfig.IMAGE_SIZE_X)
                Log.i(TAG, "imageSearch: Using ${if(_croppedBitmap.value != null) "CROPPED" else "FULL"} bitmap. Size: ${bitmap.width}x${bitmap.height}")

                var embedding = imageEmbedder.embed(bitmap)
                Log.i(TAG, "imageSearch: Embedding generated, length: ${embedding.size}")

                // Few-Shot kombinace (pokud je vybraný prototype)
                val fewShotPrototype = _selectedFewShotPrototype.value
                if (fewShotPrototype != null) {
                    Log.i(TAG, "Combining image embedding with few-shot prototype: ${fewShotPrototype.name}")
                    embedding = combineEmbeddings(
                        baseEmbedding = embedding,
                        fewShotEmbedding = fewShotPrototype.embedding,
                        fewShotWeight = 0.5f // 50% image, 50% few-shot
                    )
                    Log.i(TAG, "Combined embedding created")
                }

                search(store, embedding, threshold)
            } catch (e: Exception) {
                Log.e(TAG, "imageSearch: Error during search", e)
                _error.emit(application.getString(R.string.search_error_unknown))
            } finally {
                _isLoading.emit(false)
                imageEmbedderLastUsage = System.currentTimeMillis()
            }
        }
    }

    /**
     * Vyhledávání pouze pomocí few-shot prototype (bez text/image query)
     *
     * Použití: Kliknutí na few-shot tag → zobrazí všechny podobné obrázky
     * Např.: Tag "Barunka" → najde všechny fotky Barunky
     *
     * @param threshold Minimální similarity threshold (0.0 - 1.0), výchozí 0.2
     */
    fun fewShotSearch(threshold: Float = 0.2f) {
        val prototype = _selectedFewShotPrototype.value
        if (prototype == null) {
            Log.w(TAG, "fewShotSearch: No prototype selected")
            _error.value = "Není vybraný few-shot tag"
            return
        }

        val store = if(_mediaType.value == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }

        _isLoading.value = true
        _error.value = null
        _queryType.value = QueryType.TEXT // Nastavíme jako text query type (pro konzistentní UI)

        Log.i(TAG, "fewShotSearch: Searching with prototype '${prototype.name}'")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Použít přímo few-shot prototype embedding
                val embedding = prototype.embedding
                Log.i(TAG, "fewShotSearch: Using prototype embedding, length: ${embedding.size}")

                search(store, embedding, threshold)
            } catch (e: Exception) {
                Log.e(TAG, "fewShotSearch: Error during search", e)
                _error.emit(application.getString(R.string.search_error_unknown))
            } finally {
                _isLoading.emit(false)
            }
        }
    }

    private suspend fun search(store: FileEmbeddingStore, embedding: FloatArray, threshold: Float = 0.2f) {
        val retriever = if(_mediaType.value == MediaType.VIDEO) videoRetriever else imageRetriever
        var results = retriever.query(embedding, Int.MAX_VALUE, threshold)
        _totalResults.emit( results.size)
        results = results.take(RESULTS_BATCH_SIZE) // initial results the result loaded dynamically

        if (results.isEmpty()) {
            _error.emit(application.getString(R.string.search_error_no_results))
            _searchResults.emit(emptyList())
            return
        }

        // SDK retriever.query() už vrací výsledky seřazené podle similarity (nejvyšší first)
        // Uložíme si je v pořadí jako přišly
        val (filteredUris, idsToPurge) = results.map { embed ->
            val uri = if (_mediaType.value == MediaType.VIDEO) getVideoUriFromId(embed.id) else getImageUriFromId(embed.id)
            embed.id to uri
        }.partition { (_, uri) -> canOpenUri(application, uri) }

        if (filteredUris.isEmpty()) {
            _error.emit(application.getString(R.string.search_error_no_results))
        }

        // Uložení unfiltered výsledků (již seřazených podle podobnosti z SDK)
        val uris = filteredUris.map { it.second }
        _unfilteredSearchResults.emit(uris)

        // Aplikace tag filtru pokud jsou nějaké vybrané
        if (_selectedTagFilters.value.isNotEmpty()) {
            applyTagFilters()
        } else {
            _searchResults.emit(uris)
        }

        if(idsToPurge.isNotEmpty()){
            viewModelScope.launch(Dispatchers.IO) {
                store.remove(idsToPurge.map { it.first })
            }
        }
    }

    fun toggleViewResult(uri: Uri?){
        _resultToView.value = uri
    }

    fun openViewer(index: Int) {
        _viewerIndex.value = index
    }

    fun closeViewer() {
        _viewerIndex.value = null
    }

    fun toggleAlert(mode: MediaType){
        val isVisible = if (mode == MediaType.IMAGE) _isImageIndexAlertVisible else _isVideoIndexAlertVisible
        val hasShown = if (mode == MediaType.IMAGE) _hasShownImageIndexAlert else _hasShownVideoIndexAlert

        if (!isVisible.value && hasShown.value) return

        isVisible.value = !isVisible.value
        hasShown.value = true
    }

    fun updateQueryType(type: QueryType){
        _queryType.value = type
    }

    fun updateSearchImageUri(uri: Uri?){
        _searchImageUri.value = uri
        // Reset cropped bitmap při změně obrázku
        _croppedBitmap.value = null
    }

    /**
     * Zobrazí crop dialog
     */
    fun showCropDialog() {
        _showCropDialog.value = true
    }

    /**
     * Skryje crop dialog
     */
    fun hideCropDialog() {
        _showCropDialog.value = false
    }

    /**
     * Uloží cropped bitmap pro vyhledávání
     */
    fun setCroppedBitmap(bitmap: android.graphics.Bitmap?) {
        _croppedBitmap.value = bitmap
        if (bitmap != null) {
            Log.i(TAG, "setCroppedBitmap: Cropped bitmap saved. Size: ${bitmap.width}x${bitmap.height}")
        } else {
            Log.i(TAG, "setCroppedBitmap: Cropped bitmap cleared")
        }
    }

    /**
     * Vymaže cropped bitmap (použije se celý obrázek)
     */
    fun clearCrop() {
        _croppedBitmap.value = null
    }

    private fun shouldShutdownModel(lastUsage: Long?) = lastUsage != null && System.currentTimeMillis() - lastUsage >= modelShutdownThreshold


    fun onLoadMore() {
        if (isLoadingMoreSearchResults.getAndSet(true)) return
        val retriever = if (_mediaType.value == MediaType.VIDEO) videoRetriever else imageRetriever
        val currentItemsCount = _searchResults.value.size
        if (currentItemsCount >= _totalResults.value) return

        viewModelScope.launch(Dispatchers.IO) {
            try {

                val end = (currentItemsCount + RESULTS_BATCH_SIZE).coerceAtMost(_totalResults.value)
                val batch = retriever.query(currentItemsCount, end).take(RESULTS_BATCH_SIZE)

                val (filteredUris, idsToPurge) = batch.map { embed ->
                    embed.id to if (_mediaType.value == MediaType.VIDEO) getVideoUriFromId(embed.id) else getImageUriFromId(
                        embed.id
                    )
                }.partition { (_, uri) -> canOpenUri(application, uri) }

                if (filteredUris.isNotEmpty()) {
                    _searchResults.emit(_searchResults.value + filteredUris.map { it.second })
                }

                if (idsToPurge.isNotEmpty()) {
                    val store = if (_mediaType.value == MediaType.VIDEO) videoStore else imageStore
                    viewModelScope.launch(Dispatchers.IO) {
                        store.remove(idsToPurge.map { it.first })
                    }
                }
            }finally {
                isLoadingMoreSearchResults.set(false)
            }
        }
    }

    // Funkce pro selection mode
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedUris.value = emptySet()
        }
    }

    fun toggleUriSelection(uri: Uri) {
        val current = _selectedUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedUris.value = current
    }

    fun selectAllUris() {
        _selectedUris.value = _searchResults.value.toSet()
    }

    fun clearSelection() {
        _selectedUris.value = emptySet()
    }

    // Funkce pro přesun vybraných souborů
    suspend fun moveSelectedFiles(destinationDirUri: Uri): Pair<Int, Int> {
        val urisToMove = _selectedUris.value.toList()
        if (urisToMove.isEmpty()) return Pair(0, 0)

        val result = moveFiles(application, urisToMove, destinationDirUri)

        // Odstranění úspěšně přesunutých souborů z výsledků vyhledávání
        if (result.first > 0) {
            val movedUris = urisToMove.take(result.first).toSet()
            _searchResults.value = _searchResults.value.filterNot { movedUris.contains(it) }
            _totalResults.value = _totalResults.value - result.first
            clearSelection()
            toggleSelectionMode() // Vypnout selection mode
        }

        return result
    }

    // Funkce pro smazání vybraných souborů
    suspend fun deleteSelectedFiles(): Pair<Int, Int> {
        val urisToDelete = _selectedUris.value.toList()
        if (urisToDelete.isEmpty()) return Pair(0, 0)

        val result = deleteFiles(application, urisToDelete)

        // Odstranění úspěšně smazaných souborů z výsledků vyhledávání
        if (result.first > 0) {
            val deletedUris = urisToDelete.take(result.first).toSet()
            _searchResults.value = _searchResults.value.filterNot { deletedUris.contains(it) }
            _totalResults.value = _totalResults.value - result.first
            clearSelection()
            toggleSelectionMode() // Vypnout selection mode
        }

        return result
    }

    /**
     * Sdílí vybrané soubory pomocí Android Share Sheet.
     */
    fun shareSelectedFiles() {
        val urisToShare = _selectedUris.value.toList()
        if (urisToShare.isEmpty()) {
            Log.w(TAG, "Žádné vybrané soubory ke sdílení")
            return
        }

        val shareTitle = when (urisToShare.size) {
            1 -> application.getString(R.string.share_single_file)
            else -> application.getString(R.string.share_multiple_files, urisToShare.size)
        }

        ShareManager.shareFiles(
            context = application,
            uris = urisToShare,
            shareTitle = shareTitle
        )
    }

    // ============ TAG FILTERING ============

    /**
     * Načte dostupné tagy s počty obrázků pro filtering
     */
    fun loadAvailableTagsWithCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tagRepository.activeTags.collect { tags ->
                    val withCounts = tags.map { tag ->
                        val count = tagRepository.getMediaCountForTag(tag.name)
                        tag to count
                    }.filter { it.second > 0 }  // Pouze tagy s obrázky

                    _availableTagsWithCounts.value = withCounts
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tags with counts", e)
            }
        }
    }

    /**
     * Toggle tag filter (přidat/odebrat z vybraných)
     */
    fun toggleTagFilter(tagName: String) {
        val current = _selectedTagFilters.value.toMutableSet()
        if (current.contains(tagName)) {
            current.remove(tagName)
        } else {
            current.add(tagName)
        }
        _selectedTagFilters.value = current

        // Re-apply filter
        viewModelScope.launch(Dispatchers.IO) {
            applyTagFilters()
        }
    }

    /**
     * Vyčistí všechny tag filtry
     */
    fun clearTagFilters() {
        _selectedTagFilters.value = emptySet()
        _searchResults.value = _unfilteredSearchResults.value
    }

    /**
     * Nastaví date range filter
     */
    fun setDateRange(startDate: Long?, endDate: Long?) {
        _dateRangeStart.value = startDate
        _dateRangeEnd.value = endDate

        // Re-apply všechny filtry (tag + date)
        viewModelScope.launch(Dispatchers.IO) {
            applyAllFilters()
        }
    }

    /**
     * Vyčistí date range filter
     */
    fun clearDateRange() {
        _dateRangeStart.value = null
        _dateRangeEnd.value = null

        // Re-apply všechny filtry
        viewModelScope.launch(Dispatchers.IO) {
            applyAllFilters()
        }
    }

    /**
     * Aplikuje tag filtry na výsledky vyhledávání
     *
     * Logika: AND - zobrazit pouze obrázky, které mají VŠECHNY vybrané tagy
     */
    private suspend fun applyTagFilters() {
        applyAllFilters()
    }

    /**
     * Aplikuje všechny filtry (tag + date range) na výsledky vyhledávání
     *
     * DŮLEŽITÉ: Pokud jsou vybrané tagy, ale žádné search výsledky,
     * načte všechny obrázky s těmito tagy přímo z databáze.
     */
    private suspend fun applyAllFilters() {
        val hasTagFilter = _selectedTagFilters.value.isNotEmpty()
        val hasDateFilter = _dateRangeStart.value != null || _dateRangeEnd.value != null
        // Aktivní search = máme search výsledky k filtrování
        val hasActiveSearch = _unfilteredSearchResults.value.isNotEmpty()

        // Pokud nejsou žádné filtry, zobraz všechny výsledky
        if (!hasTagFilter && !hasDateFilter) {
            _searchResults.value = _unfilteredSearchResults.value
            _totalResults.value = _unfilteredSearchResults.value.size
            return
        }

        try {
            var filtered: List<Uri>

            // KLÍČOVÁ LOGIKA: Pokud nejsou aktivní search výsledky (text/image search),
            // načti všechny obrázky z tagů přímo z databáze
            if (hasTagFilter && !hasActiveSearch) {
                // Načíst všechny image IDs s vybranými tagy (OR logika)
                val filteredImageIds = tagRepository.getMediaIdsWithAnyTag(
                    _selectedTagFilters.value.toList()
                )

                // Převést IDs na URIs
                filtered = filteredImageIds.map { imageId ->
                    getImageUriFromId(imageId)
                }
            } else {
                // Aktivní search - filtruj pouze existující výsledky vyhledávání
                filtered = _unfilteredSearchResults.value

                // 1. Aplikuj tag filter (OR logika)
                if (hasTagFilter) {
                    val filteredImageIds = tagRepository.getMediaIdsWithAnyTag(
                        _selectedTagFilters.value.toList()
                    ).toSet()

                    filtered = filtered.filter { uri ->
                        val imageId = getImageIdFromUri(uri)
                        imageId != null && filteredImageIds.contains(imageId)
                    }
                }
            }

            // 2. Aplikuj date range filter (platí vždy)
            if (hasDateFilter) {
                val startDate = _dateRangeStart.value
                val endDate = _dateRangeEnd.value

                filtered = filtered.filter { uri ->
                    val dateAdded = getDateAddedFromUri(uri)
                    if (dateAdded == null) return@filter false

                    val matchesStart = startDate == null || dateAdded >= startDate
                    val matchesEnd = endDate == null || dateAdded <= endDate

                    matchesStart && matchesEnd
                }
            }


            // 3. Odfiltruj obrázky s excluded tagy
            // Načíst všechny excluded tagy
            val excludedTags = tagRepository.getActiveTagsSync().filter { it.isExcluded }
            if (excludedTags.isNotEmpty()) {
                val excludedTagNames = excludedTags.map { it.name }
                val excludedImageIds = tagRepository.getMediaIdsWithAnyTag(excludedTagNames).toSet()
                
                // Odebrat obrázky které mají jakýkoliv excluded tag
                filtered = filtered.filter { uri ->
                    val imageId = getImageIdFromUri(uri)
                    imageId != null && !excludedImageIds.contains(imageId)
                }
            }
            _searchResults.value = filtered
            _totalResults.value = filtered.size
        } catch (e: Exception) {
            Log.e(TAG, "Error applying filters", e)
            _searchResults.value = _unfilteredSearchResults.value
        }
    }

    /**
     * Získá DATE_TAKEN metadata z URI (timestamp v ms) - datum vytvoření fotografie
     *
     * DATE_TAKEN reprezentuje kdy byla fotografie pořízena (z EXIF metadat)
     * Pokud není k dispozici, fallback na DATE_ADDED
     */
    private fun getDateAddedFromUri(uri: Uri): Long? {
        return try {
            val projection = arrayOf(
                android.provider.MediaStore.Images.Media.DATE_TAKEN,
                android.provider.MediaStore.Images.Media.DATE_ADDED
            )
            application.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // Priorita: DATE_TAKEN (datum vytvoření fotografie)
                    val dateTakenIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATE_TAKEN)
                    val dateTaken = if (dateTakenIndex != -1) {
                        cursor.getLong(dateTakenIndex)
                    } else 0L

                    if (dateTaken > 0) {
                        dateTaken // DATE_TAKEN je už v ms
                    } else {
                        // Fallback na DATE_ADDED pokud DATE_TAKEN není k dispozici
                        val dateAddedIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATE_ADDED)
                        cursor.getLong(dateAddedIndex) * 1000 // DATE_ADDED je v sekundách, převést na ms
                    }
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting date from URI: $uri", e)
            null
        }
    }

    /**
     * Extrahuje image ID z URI
     */
    private fun getImageIdFromUri(uri: Uri): Long? {
        return try {
            android.content.ContentUris.parseId(uri)
        } catch (e: Exception) {
            null
        }
    }

    // ============ FEW-SHOT LEARNING ============

    /**
     * Načte dostupné few-shot prototypes
     */
    fun loadAvailableFewShotPrototypes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fewShotPrototypeDao.getAllPrototypes().collect { prototypes ->
                    _availableFewShotPrototypes.value = prototypes
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading few-shot prototypes", e)
            }
        }
    }

    /**
     * Vybere few-shot prototype pro vyhledávání
     */
    fun selectFewShotPrototype(prototype: FewShotPrototypeEntity?) {
        _selectedFewShotPrototype.value = prototype
        Log.i(TAG, "Few-shot prototype ${if (prototype != null) "selected: ${prototype.name}" else "deselected"}")
    }

    /**
     * Kombinuje embeddingy - průměrování s váhami
     *
     * @param baseEmbedding Hlavní embedding (text nebo image query)
     * @param fewShotEmbedding Few-shot prototype embedding
     * @param fewShotWeight Váha few-shot embeddingu (0.0 - 1.0), výchozí 0.5
     * @return Kombinovaný embedding
     */
    private fun combineEmbeddings(
        baseEmbedding: FloatArray,
        fewShotEmbedding: FloatArray,
        fewShotWeight: Float = 0.5f
    ): FloatArray {
        require(baseEmbedding.size == fewShotEmbedding.size) {
            "Embeddings must have same dimension"
        }
        require(fewShotWeight in 0f..1f) {
            "fewShotWeight must be between 0.0 and 1.0"
        }

        val baseWeight = 1f - fewShotWeight
        val combined = FloatArray(baseEmbedding.size) { i ->
            baseEmbedding[i] * baseWeight + fewShotEmbedding[i] * fewShotWeight
        }

        // Normalizace (L2 norm)
        val norm = kotlin.math.sqrt(combined.sumOf { (it * it).toDouble() }.toFloat())
        return if (norm > 0) {
            combined.map { it / norm }.toFloatArray()
        } else {
            combined
        }
    }

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        translationService.dispose()
        super.onCleared()
    }
}
