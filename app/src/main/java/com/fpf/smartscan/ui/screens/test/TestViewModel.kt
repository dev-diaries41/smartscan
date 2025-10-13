package com.fpf.smartscan.ui.screens.test

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.fpf.smartscan.R
import com.fpf.smartscansdk.core.ml.embeddings.ClassificationResult
import com.fpf.smartscansdk.core.ml.embeddings.PrototypeEmbedding
import com.fpf.smartscansdk.core.ml.embeddings.classify
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.ml.models.ResourceId
import com.fpf.smartscansdk.core.utils.getBitmapFromUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TestViewModel(application: Application) : AndroidViewModel(application){
    private var embeddingsHandler =  ClipImageEmbedder(application.resources, ResourceId(R.raw.image_encoder_quant_int8))

    private val _predictedClass = MutableStateFlow<String?>(null)
    val predictedClass: StateFlow<String?> = _predictedClass
    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    fun updateImageUri(uri: Uri?) {
        _imageUri.value = uri
    }

    fun clearInferenceResult() {
        _predictedClass.value = null
    }

    fun inference(context: Context, classPrototypes: List<PrototypeEmbedding>, threshold: Float, confidenceMargin: Float) {
        if (classPrototypes.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.test_no_prototype_embeddings), Toast.LENGTH_SHORT).show()
            return
        }
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if(!embeddingsHandler.isInitialized()) embeddingsHandler.initialize()
                val uri = _imageUri.value ?: return@launch
                val bitmap = getBitmapFromUri(context, uri, ClipConfig.CLIP_EMBEDDING_LENGTH)
                val imageEmbedding = embeddingsHandler.embed(bitmap)
                val classResult = classify(imageEmbedding, classPrototypes, threshold = threshold, confidenceMargin = confidenceMargin)

                when (classResult){
                    is ClassificationResult.Failure -> {
                        _predictedClass.emit("No match")
                        return@launch
                }
                    is ClassificationResult.Success -> {
                        val result = DocumentFile.fromTreeUri(context, classResult.classId.toUri())?.name
                        _predictedClass.emit(result)
                    }
                }
            } catch (e: Exception) {
                Log.e("TestViewModel", "Inference failed: ${e.message}", e)
            }finally {
                _isLoading.emit(false)
            }
        }
    }

    fun onDispose(){
        embeddingsHandler.closeSession()
    }

    override fun onCleared() {
        super.onCleared()
        embeddingsHandler.closeSession()
    }
}