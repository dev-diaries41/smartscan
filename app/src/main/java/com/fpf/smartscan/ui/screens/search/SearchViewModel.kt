package com.fpf.smartscan.ui.screens.search

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fpf.smartscan.data.images.ImageEmbedding
import com.fpf.smartscan.data.images.ImageEmbeddingDatabase
import com.fpf.smartscan.data.images.ImageEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.getSimilarities
import com.fpf.smartscan.lib.clip.getTopN
import com.fpf.smartscan.lib.getImageUriFromId
import com.fpf.smartscan.lib.hasImageAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.any
import com.fpf.smartscan.R
import com.fpf.smartscan.lib.clip.ModelType
import kotlinx.coroutines.CoroutineScope

class SearchViewModel(private val application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    private val workTag = "ImageBatchWorker"

    private val _progress = MutableLiveData(0f)
    val progress: LiveData<Float> = _progress

    private var embeddingsHandler: Embeddings? = null
    private val repository: ImageEmbeddingRepository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )
    val imageEmbeddings: LiveData<List<ImageEmbedding>> = repository.allImageEmbeddings

    private val _query = MutableLiveData<String>("")
    val query: LiveData<String> = _query

    private val _searchResults = MutableLiveData<List<Uri>>(emptyList())
    val searchResults: LiveData<List<Uri>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isFirstIndex = MutableLiveData<Boolean>(false)
    val isFirstIndex: LiveData<Boolean> = _isFirstIndex

    init {
        observeWorkProgress()
        CoroutineScope(Dispatchers.Default).launch {
            embeddingsHandler = Embeddings(application.resources, ModelType.TEXT)
            val indexWorkScheduled = isIndexWorkScheduled("ImageIndexWorker")
            if (!indexWorkScheduled) {
                _isFirstIndex.postValue(true)
            }
            _isLoading.postValue(false)
        }
    }

    private fun observeWorkProgress() {
        workManager.getWorkInfosByTagLiveData(workTag).observeForever { infos ->
            if (infos.isNullOrEmpty()) {
                _progress.postValue(0f)
                return@observeForever
            }

            val runningWorker = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            if (runningWorker != null) {
                val processedCount = runningWorker.progress.getInt("processed_count", 0)
                if (processedCount == 0) return@observeForever
                val totalCount = runningWorker.progress.getInt("total_count", 1).takeIf { it > 0 } ?: 1
                val normalizedProgress = processedCount.toFloat() / totalCount.toFloat()
                _progress.postValue(normalizedProgress)
            } else {
                val enqueued = infos.any { it.state == WorkInfo.State.ENQUEUED }
                if (!enqueued) {
                    _progress.postValue(0f)
                }
            }
        }
    }

    // Worker used to schedule Indexing of images every week. This allows new images to indexed frequently
    private suspend fun isIndexWorkScheduled(workName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val workInfoList = workManager.getWorkInfosForUniqueWork(workName).get()
            workInfoList.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
    }

    fun setQuery(newQuery: String) {
        _query.value = newQuery
        if(newQuery.isEmpty()){
            _error.value = null
        }
    }

    fun searchImages(n: Int, embeddings: List<ImageEmbedding>) {
        val currentQuery = _query.value
        if (currentQuery.isNullOrBlank()) {
            _error.value = application.getString(R.string.search_error_empty_query)
            return
        }

        if (embeddings.isEmpty()) {
            _error.value = application.getString(R.string.search_error_images_not_indexed)
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val textEmbedding = embeddingsHandler?.generateTextEmbedding(currentQuery)
                    ?: throw IllegalArgumentException("Failed to generate text embedding.")

                val similarities = getSimilarities(textEmbedding, embeddings.map { it.embeddings })

                if (similarities.isEmpty()) {
                    _error.value = application.getString(R.string.search_error_no_results)
                    _searchResults.value = emptyList()
                    return@launch
                }

                val results = getTopN(similarities, n, 0.2f)

                if (results.isEmpty() ) {
                    _error.value = application.getString(R.string.search_error_no_results)
                    _searchResults.value = emptyList()
                    return@launch
                }

                val searchResultsUris = results.map { idx -> getImageUriFromId(embeddings[idx].id) }
                val filteredSearchResultsUris = searchResultsUris.filter { uri ->
                    hasImageAccess(application, uri)
                }

                if (filteredSearchResultsUris.isEmpty()) {
                    _error.value = application.getString(R.string.search_error_no_results)
                    _searchResults.value = emptyList()
                    return@launch
                }

                _searchResults.value = filteredSearchResultsUris

            } catch (e: Exception) {
                Log.e("ImageSearchError", "$e")
                _error.value = application.getString(R.string.search_error_unknown)
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        embeddingsHandler?.closeSession()
    }
}
