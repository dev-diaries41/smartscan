package com.fpf.smartscan.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.fpf.smartscan.ITextEmbedderService
import com.fpf.smartscan.R
import com.fpf.smartscansdk.core.embeddings.flattenEmbeddings
import com.fpf.smartscansdk.ml.data.ResourceId
import com.fpf.smartscansdk.ml.models.providers.embeddings.clip.ClipTextEmbedder
import kotlinx.coroutines.runBlocking


class TextEmbedderAidlService: Service() {

    companion object {
        const val TAG = "TextEmbedderAidlService"
    }
    private lateinit var textEmbedder: ClipTextEmbedder

    override fun onCreate() {
        super.onCreate()
        textEmbedder = ClipTextEmbedder(application, ResourceId(R.raw.text_encoder_quant_int8))
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
    }
}