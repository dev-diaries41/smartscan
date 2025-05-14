package com.fpf.smartscan.ui.screens.search

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import com.fpf.smartscan.data.images.ImageEmbedding
import com.fpf.smartscan.data.images.ImageEmbeddingDatabase
import com.fpf.smartscan.data.images.ImageEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.getSimilarities
import com.fpf.smartscan.lib.clip.getTopN
import com.fpf.smartscan.lib.getImageUriFromId
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.data.videos.VideoEmbedding
import com.fpf.smartscan.data.videos.VideoEmbeddingDatabase
import com.fpf.smartscan.data.videos.VideoEmbeddingRepository
import com.fpf.smartscan.lib.canOpenUri
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.getVideoUriFromId
import com.fpf.smartscan.lib.processors.ImageIndexListener
import com.fpf.smartscan.lib.processors.VideoIndexListener
import com.fpf.smartscan.services.MediaIndexForegroundService
import kotlinx.coroutines.CoroutineScope

enum class SearchMode {
    IMAGE, VIDEO
}

val searchModeOptions = mapOf(
    SearchMode.IMAGE to "Images",
    SearchMode.VIDEO to "Videos",
)

class SearchViewModel(private val application: Application) : AndroidViewModel(application) {
    val videoIndexProgress = VideoIndexListener.progress
    val isIndexingVideos = VideoIndexListener.indexingInProgress
    val imageIndexProgress = ImageIndexListener.progress
    val isIndexingImages = ImageIndexListener.indexingInProgress

    private var embeddingsHandler: Embeddings? = null
    private val repository: ImageEmbeddingRepository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )
    private val videoRepository: VideoEmbeddingRepository = VideoEmbeddingRepository(
        VideoEmbeddingDatabase.getDatabase(application).videoEmbeddingDao()
    )
    val imageEmbeddings: LiveData<List<ImageEmbedding>> = repository.allImageEmbeddings
    val videoEmbeddings: LiveData<List<VideoEmbedding>> = videoRepository.allVideoEmbeddings

    val hasAnyImages: LiveData<Boolean> = repository.hasAnyEmbedding
    val hasAnyVideos: LiveData<Boolean> = videoRepository.hasAnyVideoEmbeddings

    private val _query = MutableLiveData<String>("")
    val query: LiveData<String> = _query

    private val _searchResults = MutableLiveData<List<Uri>>(emptyList())
    val searchResults: LiveData<List<Uri>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _mode = MutableLiveData<SearchMode>(SearchMode.IMAGE)
    val mode: LiveData<SearchMode> = _mode

    fun setQuery(newQuery: String) {
        _query.value = newQuery
        if(newQuery.isEmpty()){
            _error.value = null
        }
    }

    fun clearResults(){
        _searchResults.value = emptyList()
    }

    fun setMode(newMode: SearchMode) {
        _mode.value = newMode
        _error.value = null
        _searchResults.value = emptyList()
    }

    fun searchImages(n: Int, embeddings: List<ImageEmbedding>, threshold: Float = 0.2f) {
        val currentQuery = _query.value
        if (currentQuery.isNullOrBlank()) {
            _error.value = application.getString(R.string.search_error_empty_query)
            return
        }

        if (embeddings.isEmpty()) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }

        _isLoading.value = true
        _error.value = null

        CoroutineScope(Dispatchers.Default).launch {
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

                val searchResultsUris = results.map { idx -> getImageUriFromId(embeddings[idx].id) }
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

    fun searchVideos(n: Int, embeddings: List<VideoEmbedding>, threshold: Float = 0.2f) {
        val currentQuery = _query.value
        if (currentQuery.isNullOrBlank()) {
            _error.value = application.getString(R.string.search_error_empty_query)
            return
        }

        if (embeddings.isEmpty()) {
            _error.value = application.getString(R.string.search_error_not_indexed)
            return
        }

        _isLoading.value = true
        _error.value = null

        CoroutineScope(Dispatchers.Default).launch {
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

                val searchResultsUris = results.map { idx -> getVideoUriFromId(embeddings[idx].id) }
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



    override fun onCleared() {
        embeddingsHandler?.closeSession()
        super.onCleared()
    }
}
