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

    private val textEmbedder = ClipTextEmbedder(application.resources, ResourceId(R.raw.text_encoder_quant_int8))
    private val imageEmbedder = ClipImageEmbedder(application.resources, ResourceId(R.raw.image_encoder_quant_int8))

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
                MediaType.IMAGE -> anyImages || imageStore.exists
                MediaType.VIDEO -> anyVideos || videoStore.exists
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

    private val _queryType = MutableStateFlow(QueryType.TEXT)
    val queryType: StateFlow<QueryType> = _queryType

    private val _searchImageUri = MutableStateFlow<Uri?>(null)
    val searchImageUri: StateFlow<Uri?> = _searchImageUri

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

        // saves memory by lazy loading video index
        // This check is only valid if useCache true, which is default
        if(newMode == MediaType.VIDEO && !videoStore.isCached){
            viewModelScope.launch(Dispatchers.IO){loadVideoIndex()}
        }
    }

    private fun reset(){
        _error.value = null
        _query.value = ""
        clearResults()
    }


    fun textSearch(n: Int, threshold: Float = 0.2f) {
        val currentQuery = _query.value
        if (currentQuery.isBlank()) {
            _error.value = application.getString(R.string.search_error_empty_query)
            return
        }

        val store = if(_mode.value == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!textEmbedder.isInitialized()){
                    textEmbedder.initialize()
                }
                val embedding = textEmbedder.embed(currentQuery)
                search(store, embedding, n, threshold)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _error.emit(application.getString(R.string.search_error_unknown))
            } finally {
                _isLoading.emit(false)
            }
        }
    }

    fun imageSearch(n: Int, threshold: Float = 0.2f) {
        if (_searchImageUri.value == null) return

        val store = if(_mode.value == MediaType.VIDEO) videoStore else imageStore
        if(!store.exists) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if(!imageEmbedder.isInitialized()){
                    imageEmbedder.initialize()
                }
                val bitmap = getBitmapFromUri(application, _searchImageUri.value!!, ClipConfig.IMAGE_SIZE_X)
                val embedding = imageEmbedder.embed(bitmap)
                search(store, embedding, n, threshold)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                _error.emit(application.getString(R.string.search_error_unknown))
            } finally {
                _isLoading.emit(false)
            }
        }
    }

    private suspend fun search(store: FileEmbeddingStore, embedding: FloatArray, n: Int, threshold: Float = 0.2f) {
        _isLoading.value = true
        _error.value = null

        val retriever = if(_mode.value == MediaType.VIDEO) videoRetriever else imageRetriever
        val results = retriever.query(embedding, n, threshold)

        if (results.isEmpty()) {
            _error.emit(application.getString(R.string.search_error_no_results))
            _searchResults.emit(emptyList())
            return
        }

        val (filteredUris, idsToPurge) = results.map { embed ->
            val uri = if (_mode.value == MediaType.VIDEO) getVideoUriFromId(embed.id) else getImageUriFromId(embed.id)
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

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
