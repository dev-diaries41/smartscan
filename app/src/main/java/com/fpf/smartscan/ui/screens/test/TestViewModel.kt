package com.fpf.smartscan.ui.screens.test

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.prototypes.PrototypeEmbedding
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.getSimilarities
import com.fpf.smartscan.lib.clip.getTopN
import com.fpf.smartscan.lib.getBitmapFromUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.fpf.smartscan.R

class TestViewModel(application: Application) : AndroidViewModel(application){
    private var embeddingsHandler: Embeddings? = null

    private val _predictedClass = MutableLiveData<String?>()
    val predictedClass: LiveData<String?> get() = _predictedClass

    private val _imageUri = MutableLiveData<Uri?>(null)
    val imageUri: LiveData<Uri?> = _imageUri

    init {
        CoroutineScope(Dispatchers.Default).launch {
            embeddingsHandler = Embeddings(application.resources)
        }
    }

    fun updateImageUri(uri: Uri?) {
        _imageUri.value = uri
    }

    fun clearInferenceResult() {
        _predictedClass.postValue(null)
    }

    fun inference(context: Context, prototypeEmbeddings:  List<PrototypeEmbedding>) {
        if (prototypeEmbeddings.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.test_no_prototype_embeddings), Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = _imageUri.value ?: return@launch
                val bitmap = getBitmapFromUri(context, uri)
                val imageEmbedding = embeddingsHandler?.generateImageEmbedding(bitmap) ?: return@launch
                val similarities = getSimilarities(imageEmbedding, prototypeEmbeddings.map { it.embeddings })
                val bestIndex = getTopN(similarities, 1, 0.2f).firstOrNull() ?: -1
                val result = prototypeEmbeddings.getOrNull(bestIndex)?.id?.let {
                    DocumentFile.fromTreeUri(context, it.toUri())?.name ?: "Unknown"
                } ?: "Unknown"
                _predictedClass.postValue(result)
            } catch (e: Exception) {
                Log.e("TestViewModelError", "Inference failed: ${e.message}", e)
            }
        }

    }
    override fun onCleared() {
        super.onCleared()
        embeddingsHandler?.closeSession()
    }
}