package com.fpf.smartscan.ui.screens.search

import android.app.Application
import android.content.Intent
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
import com.fpf.smartscan.data.videos.VideoEmbeddingDatabase
import com.fpf.smartscan.data.videos.VideoEmbeddingRepository
import com.fpf.smartscan.lib.canOpenUri
import com.fpf.smartscan.lib.getVideoUriFromId
import com.fpf.smartscan.lib.ImageIndexListener
import com.fpf.smartscan.lib.VideoIndexListener
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.CLIP_EMBEDDING_LENGTH
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipTextEmbedder
import com.fpf.smartscansdk.core.ml.models.ResourceId
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

enum class MediaType {
    IMAGE, VIDEO
}

val searchModeOptions = mapOf(
    MediaType.IMAGE to "Images",
    MediaType.VIDEO to "Videos",
)

class SearchViewModel(private val application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SearchViewModel"
    }

    val imageIndexProgress = ImageIndexListener.progress
    val imageIndexStatus = ImageIndexListener.indexingStatus
    val videoIndexProgress = VideoIndexListener.progress
    val videoIndexStatus = VideoIndexListener.indexingStatus

    val imageStore = FileEmbeddingStore(application.filesDir, ImageIndexer.INDEX_FILENAME, CLIP_EMBEDDING_LENGTH)
    val imageRetriever = FileEmbeddingRetriever(imageStore)

    val videoStore = FileEmbeddingStore(application.filesDir,  VideoIndexer.INDEX_FILENAME, CLIP_EMBEDDING_LENGTH )
    val videoRetriever = FileEmbeddingRetriever(videoStore)

    private val embeddingsHandler = ClipTextEmbedder(application.resources, ResourceId(R.raw.text_encoder_quant_int8))
    private val repository: ImageEmbeddingRepository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )
    private val videoRepository: VideoEmbeddingRepository = VideoEmbeddingRepository(
        VideoEmbeddingDatabase.getDatabase(application).videoEmbeddingDao()
    )

    private val _hasRefreshedImageIndex = MutableStateFlow<Boolean>(false)
    private val _hasRefreshedVideoIndex = MutableStateFlow<Boolean>(false)

    private val _hasShownImageIndexAlert = MutableStateFlow<Boolean>(false)
    private val _hasShownVideoIndexAlert = MutableStateFlow<Boolean>(false)
    private val _isVideoIndexAlertVisible = MutableStateFlow<Boolean>(false)
    val isVideoIndexAlertVisible: StateFlow<Boolean> = _isVideoIndexAlertVisible
    private val _isImageIndexAlertVisible = MutableStateFlow<Boolean>(false)
    val isImageIndexAlertVisible: StateFlow<Boolean> = _isImageIndexAlertVisible

    private val _mode = MutableStateFlow<MediaType>(MediaType.IMAGE)
    val mode: StateFlow<MediaType> = _mode

    private val hasAnyImages: Flow<Boolean> = repository.hasAnyEmbedding
    private val hasAnyVideos: Flow<Boolean> = videoRepository.hasAnyVideoEmbeddings
    val hasIndexed: StateFlow<Boolean?> =
        combine(_mode, hasAnyImages, hasAnyVideos) { mode, anyImages, anyVideos ->
            when (mode) {
                MediaType.IMAGE -> (anyImages == true) || imageStore.exists
                MediaType.VIDEO -> (anyVideos == true) || videoStore.exists
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _query = MutableStateFlow<String>("")
    val query: StateFlow<String> = _query

    private val _searchResults = MutableStateFlow<List<Uri>>(emptyList())
    val searchResults: StateFlow<List<Uri>> = _searchResults

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _canSearchImages = MutableStateFlow<Boolean>(false)
    private val _canSearchVideos = MutableStateFlow<Boolean>(false)
    val canSearch: StateFlow<Boolean> =
        combine(_mode, _canSearchImages, _canSearchVideos) { mode, canImages, canVideos ->
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
                    store.getAll()
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
        if(mode == MediaType.IMAGE && _hasRefreshedImageIndex.value == false){
            loadImageIndex()
            _hasRefreshedImageIndex.value = true
        }else if (mode == MediaType.VIDEO && _hasRefreshedVideoIndex.value == false){
            loadVideoIndex()
            _hasRefreshedVideoIndex.value = true
        }
    }

    fun setQuery(newQuery: String) {
        _query.value = newQuery
        if(newQuery.isEmpty()){
            _error.value = null
        }
    }

    fun clearResults(){
        _searchResults.value = emptyList()
    }

    fun setMode(newMode: MediaType) {
        _mode.value = newMode
        reset()

        // saves memory by lazy loading video index ensure its not prematurely loaded
        if(newMode == MediaType.VIDEO && !videoStore.isLoaded){
            viewModelScope.launch(Dispatchers.IO){loadVideoIndex()}
        }
    }

    private fun reset(){
        _error.value = null
        _query.value = ""
        clearResults()
    }


    fun search(n: Int, threshold: Float = 0.2f) {
        val currentQuery = _query.value
        if (currentQuery.isBlank()) {
            _error.value = application.getString(R.string.search_error_empty_query)
            return
        }

        //TODO: Replace this with `.exist` on stores as single source of truth

        val store = if(_mode.value == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!embeddingsHandler.isInitialized()){
                    embeddingsHandler.initialize()
                }

                val textEmbedding = embeddingsHandler.embed(currentQuery)
                val retriever = if(_mode.value == MediaType.VIDEO) videoRetriever else imageRetriever
                val results = retriever.query(textEmbedding, n, threshold)

                if (results.isEmpty()) {
                    _error.emit(application.getString(R.string.search_error_no_results))
                    _searchResults.emit(emptyList())
                    return@launch
                }

                val searchResultsUris = if(_mode.value == MediaType.VIDEO) {
                    results.map { embed -> getVideoUriFromId(embed.id) }}
                else {
                    results.map { embed -> getImageUriFromId(embed.id ) }
                }
                val filteredSearchResultsUris = searchResultsUris.filter { uri ->
                    canOpenUri(application, uri)
                }

                if (filteredSearchResultsUris.isEmpty()) {
                    _error.emit(application.getString(R.string.search_error_no_results))
                    _searchResults.emit(emptyList())
                    return@launch
                }

                _searchResults.emit(filteredSearchResultsUris)

            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _error.emit(application.getString(R.string.search_error_unknown))
            } finally {
                _isLoading.emit(false)
            }
        }
    }

    fun startIndexing(mediaType: MediaType) {
        Intent(application, MediaIndexForegroundService::class.java)
            .putExtra(
                MediaIndexForegroundService.EXTRA_MEDIA_TYPE,
               if(mediaType == MediaType.VIDEO)  MediaIndexForegroundService.TYPE_VIDEO else MediaIndexForegroundService.TYPE_IMAGE
            ).also { intent ->
                application.startForegroundService(intent)
            }
    }

    fun toggleViewResult(uri: Uri?){
        _resultToView.value = uri
    }

    fun toggleAlert(mode: MediaType){
        if(mode == MediaType.IMAGE){
            if(_isImageIndexAlertVisible.value == false && _hasShownImageIndexAlert.value == true ) return
            _isImageIndexAlertVisible.value = !_isImageIndexAlertVisible.value
            _hasShownImageIndexAlert.value = true
        }else if(mode == MediaType.VIDEO){
            if(_isVideoIndexAlertVisible.value == false && _hasShownVideoIndexAlert.value == true ) return
            _isVideoIndexAlertVisible.value = !_isVideoIndexAlertVisible.value
            _hasShownVideoIndexAlert.value = true
        }
    }

    override fun onCleared() {
        embeddingsHandler.closeSession()
        super.onCleared()
    }
}
