package com.fpf.smartscan.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.fpf.smartscan.ITextEmbedderService
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.miniLmTextEmbedderModel
import com.fpf.smartscan.data.SmartScanModelType
import com.fpf.smartscan.lib.getImportedModels
import com.fpf.smartscan.lib.getModelPathMap
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.flattenEmbeddings
import com.fpf.smartscansdk.ml.data.FilePath
import com.fpf.smartscansdk.ml.data.ResourceId
import com.fpf.smartscansdk.ml.models.providers.embeddings.clip.ClipTextEmbedder
import com.fpf.smartscansdk.ml.models.providers.embeddings.minilm.MiniLMTextEmbedder
import kotlinx.coroutines.runBlocking
import java.io.File


class TextEmbedderAidlService: Service() {

    companion object {
        const val TAG = "TextEmbedderAidlService"
        private const val DEFAULT_MODEL = "Default"
    }
    private lateinit var textEmbedder: TextEmbeddingProvider

    private var selectedModel = DEFAULT_MODEL

    override fun onCreate() {
        super.onCreate()
        textEmbedder = ClipTextEmbedder(application, ResourceId(R.raw.clip_text_encoder_quant))
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        textEmbedder.closeSession()
    }

    private val binder = object : ITextEmbedderService.Stub() {

        override fun getEmbeddingDim(): Int {
            return textEmbedder.embeddingDim
        }

        override fun closeSession() {
            textEmbedder.closeSession()
        }

        override fun embed(data: String): FloatArray? {
            return runBlocking {
                try {
                    if (!textEmbedder.isInitialized()) textEmbedder.initialize()
                    val embedding = textEmbedder.embed(data)
                    embedding
                } catch (e: Exception) {
                    Log.d(TAG, "EMBEDDING_ERROR: ${e.message}")
                    null
                }
            }
        }

        override fun embedBatch(data: List<String>): FloatArray? {
            return runBlocking {
                try {
                    if(!textEmbedder.isInitialized()) textEmbedder.initialize()
                    val embeddings = textEmbedder.embedBatch(data)
                    val flattenedEmbeddings = flattenEmbeddings(embeddings, textEmbedder.embeddingDim)
                    flattenedEmbeddings
                }catch(e: Exception){
                    Log.d(TAG, "EMBEDDING_ERROR: ${e.message}")
                    null
                }
            }
        }

        override fun listModels(): List<String> {
            return getImportedModels(application).filter { it.type == SmartScanModelType.TEXT_ENCODER }.map { it.name }
        }

        override fun selectModel(model: String): Boolean {
            if(model == selectedModel) return true

            val availableModels = listModels() + DEFAULT_MODEL
            if(!availableModels.contains(model)) return false

            selectedModel = model

            val modelPathsMap = getModelPathMap()
            val pathInfo = modelPathsMap[model]

            textEmbedder = when(model){
                miniLmTextEmbedderModel.name -> {
                    textEmbedder.closeSession()
                    MiniLMTextEmbedder(application, FilePath(File(application.filesDir, pathInfo!!.path).path))
                }
                DEFAULT_MODEL -> {
                    textEmbedder.closeSession()
                    ClipTextEmbedder(application, ResourceId(R.raw.clip_text_encoder_quant))
                }
                else -> return false
            }
            return true
        }
    }
}