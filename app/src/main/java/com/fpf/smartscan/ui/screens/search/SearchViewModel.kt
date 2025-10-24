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
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.data.QueryType
import com.fpf.smartscan.data.videos.VideoEmbeddingDatabase
import com.fpf.smartscan.data.videos.VideoEmbeddingRepository
import com.fpf.smartscan.lib.canOpenUri
import com.fpf.smartscan.lib.getVideoUriFromId
import com.fpf.smartscan.lib.ImageIndexListener
import com.fpf.smartscan.lib.VideoIndexListener
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

    private val repository: ImageEmbeddingRepository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )
    private val videoRepository: VideoEmbeddingRepository = VideoEmbeddingRepository(
        VideoEmbeddingDatabase.getDatabase(application).videoEmbeddingDao()
    )

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

    private val _queryType = MutableStateFlow(QueryType.TEXT)
    val queryType: StateFlow<QueryType> = _queryType

    private val _searchImageUri = MutableStateFlow<Uri?>(null)
    val searchImageUri: StateFlow<Uri?> = _searchImageUri

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

    fun clearResults(){
        _searchResults.value = emptyList()
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
        clearResults()
    }


    fun textSearch(query: String, threshold: Float = 0.2f) {
        if (query.isBlank()) {
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
                if(!textEmbedder.isInitialized()){
                    textEmbedder.initialize()
                }
                if(shouldShutdownModel(imageEmbedderLastUsage)) imageEmbedder.closeSession() // prevent keeping both models open
                val embedding = textEmbedder.embed(query)
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
        if (_searchImageUri.value == null) return

        val store = if(_mediaType.value == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!imageEmbedder.isInitialized()){
                    imageEmbedder.initialize()
                }
                if(shouldShutdownModel(textEmbedderLastUsage)) textEmbedder.closeSession() // prevent keeping both models open
                val bitmap = getBitmapFromUri(application, _searchImageUri.value!!, ClipConfig.IMAGE_SIZE_X)
                val embedding = imageEmbedder.embed(bitmap)
                search(store, embedding, threshold)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _error.emit(application.getString(R.string.search_error_unknown))
            } finally {
                _isLoading.emit(false)
                imageEmbedderLastUsage = System.currentTimeMillis()
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

        val (filteredUris, idsToPurge) = results.map { embed ->
            val uri = if (_mediaType.value == MediaType.VIDEO) getVideoUriFromId(embed.id) else getImageUriFromId(embed.id)
            embed.id to uri
        }.partition { (_, uri) -> canOpenUri(application, uri) }

        if (filteredUris.isEmpty()) {
            _error.emit(application.getString(R.string.search_error_no_results))
        }

        _searchResults.emit(filteredUris.map { it.second })

        if(idsToPurge.isNotEmpty()){
            viewModelScope.launch(Dispatchers.IO) {
                store.remove(idsToPurge.map { it.first })
            }
        }
    }

    fun toggleViewResult(uri: Uri?){
        _resultToView.value = uri
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

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
