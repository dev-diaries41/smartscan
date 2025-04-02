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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.any
import com.fpf.smartscan.R

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
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
        CoroutineScope(Dispatchers.Default).launch {
            embeddingsHandler = Embeddings(application.resources)
            val indexWorkScheduled = isIndexWorkScheduled(application, "ImageIndexWorker")
            if (!indexWorkScheduled) {
                _isFirstIndex.postValue(true)
            }
            _isLoading.postValue(false)
        }
    }


    // Worker used to schedule Indexing of images every week. This allows new images to indexed frequently
    private suspend fun isIndexWorkScheduled(context: Context, workName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val workManager = WorkManager.getInstance(context)
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
            _error.value = context.getString(R.string.search_error_empty_query)
            return
        }

        if (embeddings.isEmpty()) {
            _error.value = context.getString(R.string.search_error_images_not_indexed)
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
                    _error.value = context.getString(R.string.search_error_no_results)
                    _searchResults.value = emptyList()
                    return@launch
                }

                val results = getTopN(similarities, n, 0.2f)

                if (results.isEmpty() ) {
                    _error.value = context.getString(R.string.search_error_no_image_found)
                    _searchResults.value = emptyList()
                    return@launch
                }

                val searchResultsUris = results.map{idx -> getImageUriFromId(embeddings[idx].id)}

                val missingPermission = searchResultsUris.any { uri ->
                    !hasImageAccess(getApplication(), uri)
                }

                if (missingPermission) {
                    _error.value = context.getString(R.string.search_error_missing_permissions_for_results)
                    return@launch
                }
                _searchResults.value = searchResultsUris
            } catch (e: Exception) {
                Log.e("ImageSearchError", "$e")
                _error.value = context.getString(R.string.search_error_unknown)
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
