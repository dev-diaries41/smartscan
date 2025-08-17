package com.fpf.smartscan.ui.screens.search

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.images.ImageEmbeddingDatabase
import com.fpf.smartscan.data.images.ImageEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.getSimilarities
import com.fpf.smartscan.lib.clip.getTopN
import com.fpf.smartscan.lib.getImageUriFromId
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.data.videos.VideoEmbeddingDatabase
import com.fpf.smartscan.data.videos.VideoEmbeddingRepository
import com.fpf.smartscan.lib.canOpenUri
import com.fpf.smartscan.lib.clip.Embedding
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.clip.loadEmbeddingsFromFile
import com.fpf.smartscan.lib.getVideoUriFromId
import com.fpf.smartscan.lib.processors.ImageIndexListener
import com.fpf.smartscan.lib.processors.ImageIndexer
import com.fpf.smartscan.lib.processors.VideoIndexListener
import com.fpf.smartscan.lib.processors.VideoIndexer
import com.fpf.smartscan.services.MediaIndexForegroundService
import java.io.File

enum class MediaType {
    IMAGE, VIDEO
}

val searchModeOptions = mapOf(
    MediaType.IMAGE to "Images",
    MediaType.VIDEO to "Videos",
)

class SearchViewModel(private val application: Application) : AndroidViewModel(application) {
    val imageIndexProgress = ImageIndexListener.progress
    val imageIndexStatus = ImageIndexListener.indexingStatus
    val videoIndexProgress = VideoIndexListener.progress
    val videoIndexStatus = VideoIndexListener.indexingStatus

    private var embeddingsHandler: Embeddings? = null
    private val repository: ImageEmbeddingRepository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )
    private val videoRepository: VideoEmbeddingRepository = VideoEmbeddingRepository(
        VideoEmbeddingDatabase.getDatabase(application).videoEmbeddingDao()
    )
    var imageEmbeddings: List<Embedding> = emptyList()
    var videoEmbeddings: List<Embedding> = emptyList()

    private val _hasRefreshedImageIndex = MutableLiveData<Boolean>(false)
    private val _hasRefreshedVideoIndex = MutableLiveData<Boolean>(false)

    private val _mode = MutableLiveData<MediaType>(MediaType.IMAGE)
    val mode: LiveData<MediaType> = _mode

    private val hasAnyImages: LiveData<Boolean> = repository.hasAnyEmbedding
    private val hasAnyVideos: LiveData<Boolean> = videoRepository.hasAnyVideoEmbeddings
    val hasIndexed = MediatorLiveData<Boolean>().apply {
        fun update() {
            val (fileHasImages, fileHasVideos) = checkHasIndexed()
            value = when (_mode.value) {
                MediaType.IMAGE -> (hasAnyImages.value == true) || fileHasImages
                MediaType.VIDEO -> (hasAnyVideos.value == true) || fileHasVideos
                else -> false
            }
        }

        addSource(_mode) { update() }
        addSource(hasAnyImages) { update() }
        addSource(hasAnyVideos) { update() }
    }


    private val _query = MutableLiveData<String>("")
    val query: LiveData<String> = _query

    private val _searchResults = MutableLiveData<List<Uri>>(emptyList())
    val searchResults: LiveData<List<Uri>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _canSearchImages = MutableLiveData<Boolean>(false)
    private val _canSearchVideos = MutableLiveData<Boolean>(false)
    val canSearch = MediatorLiveData<Boolean>().apply {
        fun update() {
            value = when (_mode.value) {
                MediaType.IMAGE -> _canSearchImages.value == true
                MediaType.VIDEO -> _canSearchVideos.value == true
                else -> false
            }
        }

        addSource(_mode) { update() }
        addSource(_canSearchImages) { update() }
        addSource(_canSearchVideos) { update() }
    }

    private val _resultToView = MutableLiveData<Uri?>()
    val resultToView: LiveData<Uri?> = _resultToView

    init {
        loadImageIndex()
    }


    private fun loadImageIndex(){
        viewModelScope.launch(Dispatchers.IO){
            _isLoading.postValue(true)
            val file = File(application.filesDir, ImageIndexer.INDEX_FILENAME)
            imageEmbeddings = if(file.exists()){
                loadEmbeddingsFromFile(file)
            }else{
                repository.getAllEmbeddingsWithFileSync(file)
            }
            if(imageEmbeddings.isNotEmpty()){
                _canSearchImages.postValue(true)
            }
            _isLoading.postValue(false)
        }
    }

    private fun loadVideoIndex(){
        viewModelScope.launch(Dispatchers.IO){
            _isLoading.postValue(true)
            val file = File(application.filesDir, VideoIndexer.INDEX_FILENAME)
            videoEmbeddings = if(file.exists()){
                loadEmbeddingsFromFile(file)
            }else{
                videoRepository.getAllEmbeddingsWithFileSync(file)
            }
            if(videoEmbeddings.isNotEmpty()){
                _canSearchVideos.postValue(true)
            }
            _isLoading.postValue(false)
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
        _error.value = null
        _searchResults.value = emptyList()
        if(newMode == MediaType.VIDEO && videoEmbeddings.isEmpty()){
            loadVideoIndex()
        }
    }


    fun search(n: Int, threshold: Float = 0.2f) {
        val currentQuery = _query.value
        if (currentQuery.isNullOrBlank()) {
            _error.value = application.getString(R.string.search_error_empty_query)
            return
        }

        val embeddings = if(_mode.value == MediaType.VIDEO) videoEmbeddings else imageEmbeddings

        if (embeddings.isEmpty()) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch((Dispatchers.IO)) {
            try {
                if (embeddingsHandler == null) {
                    embeddingsHandler = Embeddings(application.resources, ModelType.TEXT)
                }
                val textEmbedding = embeddingsHandler?.generateTextEmbedding(currentQuery)
                    ?: throw IllegalArgumentException("Failed to generate text embedding.")

                val similarities = getSimilarities(textEmbedding, embeddings.map { it.embeddings })

                if (similarities.isEmpty()) {
                    _error.postValue(application.getString(R.string.search_error_no_results))
                    _searchResults.postValue(emptyList())
                    return@launch
                }

                val results = getTopN(similarities, n, threshold)

                if (results.isEmpty()) {
                    _error.postValue(application.getString(R.string.search_error_no_results))
                    _searchResults.postValue(emptyList())
                    return@launch
                }

                val searchResultsUris = if(_mode.value == MediaType.VIDEO) {
                    results.map { idx -> getVideoUriFromId(embeddings[idx].id) }}
                else {
                    results.map { idx -> getImageUriFromId(embeddings[idx].id) }
                }
                val filteredSearchResultsUris = searchResultsUris.filter { uri ->
                    canOpenUri(application, uri)
                }

                if (filteredSearchResultsUris.isEmpty()) {
                    _error.postValue(application.getString(R.string.search_error_no_results))
                    _searchResults.postValue(emptyList())
                    return@launch
                }

                _searchResults.postValue(filteredSearchResultsUris)

            } catch (e: Exception) {
                Log.e("SearchViewModel", "$e")
                _error.postValue(application.getString(R.string.search_error_unknown))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun startIndexing() {
        Intent(application, MediaIndexForegroundService::class.java)
            .putExtra(
                MediaIndexForegroundService.EXTRA_MEDIA_TYPE,
                MediaIndexForegroundService.TYPE_IMAGE
            ).also { intent ->
                application.startForegroundService(intent)
            }
    }

    fun startVideoIndexing() {
        Intent(application, MediaIndexForegroundService::class.java)
            .putExtra(
                MediaIndexForegroundService.EXTRA_MEDIA_TYPE,
                MediaIndexForegroundService.TYPE_VIDEO
            ).also { intent ->
                application.startForegroundService(intent)
            }
    }

    fun toggleViewResult(uri: Uri?){
        _resultToView.value = uri
    }

    private fun checkHasIndexed(): Pair<Boolean, Boolean>{
        val imageIndexFile = File(application.filesDir, ImageIndexer.INDEX_FILENAME)
        val videoIndexFile = File(application.filesDir, VideoIndexer.INDEX_FILENAME)
        return Pair<Boolean, Boolean>(imageIndexFile.exists(), videoIndexFile.exists())
    }

    override fun onCleared() {
        embeddingsHandler?.closeSession()
        super.onCleared()
    }
}
