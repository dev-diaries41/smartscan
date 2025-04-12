package com.fpf.smartscan.lib.clip

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.JsonReader
import android.util.Log
import com.fpf.smartscan.R
import com.fpf.smartscan.lib.centerCrop
import com.fpf.smartscan.lib.preProcess
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.LongBuffer
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.use

enum class ModelType {
    IMAGE, TEXT, BOTH
}

class Embeddings(resources: Resources, modelType: ModelType = ModelType.BOTH) {
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var imageSession: OrtSession? = null
    private var textSession: OrtSession? = null


    private val tokenizerVocab: Map<String, Int> = getVocab(resources)
    private val tokenizerMerges: HashMap<Pair<String, String>, Int> = getMerges(resources)
    private val tokenBOS: Int = 49406
    private val tokenEOS: Int = 49407
    private val tokenizer = ClipTokenizer(tokenizerVocab, tokenizerMerges)

    init {
        if (modelType == ModelType.BOTH || modelType == ModelType.IMAGE) {
            imageSession = loadModel(resources, R.raw.image_encoder_quant_int8, "Image")
        }
        if (modelType == ModelType.BOTH || modelType == ModelType.TEXT) {
            textSession = loadModel(resources, R.raw.text_encoder_quant_int8, "Text")
        }
    }


    private fun loadModel(resources: Resources, resourceId: Int, modelName: String): OrtSession {
        lateinit var session: OrtSession
        val timeTaken = measureTimeMillis {
            val modelBytes = resources.openRawResource(resourceId).readBytes()
            session = ortEnv.createSession(modelBytes)
        }
        Log.i("ClipEmbeddings", "$modelName model loaded in ${timeTaken}ms")
        return session
    }

    suspend fun generateImageEmbedding(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        val session = imageSession ?: throw IllegalStateException("Image model not loaded")
        val processedBitmap = centerCrop(bitmap, 224)
        val inputShape = longArrayOf(1, 3, 224, 224)
        val inputName = session.inputNames.iterator().next()
        val imgData = preProcess(processedBitmap)

        OnnxTensor.createTensor(ortEnv, imgData, inputShape).use { inputTensor ->
            session.run(Collections.singletonMap(inputName, inputTensor)).use { output ->
                @Suppress("UNCHECKED_CAST")
                val rawOutput = (output[0].value as Array<FloatArray>)[0]
                normalizeL2(rawOutput)
            }
        }
    }

    suspend fun generateTextEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        val session = textSession ?: throw IllegalStateException("Text model not loaded")
        val textClean = Regex("[^A-Za-z0-9 ]").replace(text, "").lowercase()
        var tokens = mutableListOf(tokenBOS) + tokenizer.encode(textClean) + tokenEOS
        tokens = tokens.take(77) + List(77 - tokens.size) { 0 }

        val inputShape = longArrayOf(1, 77)
        val inputIds = LongBuffer.allocate(1 * 77).apply {
            tokens.forEach { put(it.toLong()) }
            rewind()
        }

        val inputName = session.inputNames?.iterator()?.next()

        OnnxTensor.createTensor(ortEnv, inputIds, inputShape).use { inputTensor ->
            session.run(Collections.singletonMap(inputName, inputTensor)).use { output ->
                @Suppress("UNCHECKED_CAST")
                val rawOutput = (output[0].value as Array<FloatArray>)[0]
                normalizeL2(rawOutput)
            }
        }
    }

    suspend fun generatePrototypeEmbedding(bitmaps: List<Bitmap>): FloatArray = withContext(Dispatchers.Default) {
        if (bitmaps.isEmpty()) {
            throw IllegalArgumentException("Bitmap list is empty")
        }

        val semaphore = Semaphore(3)
        val embeddingDeferred: List<Deferred<FloatArray?>> = bitmaps.map { bitmap ->
            async {
                semaphore.withPermit {
                    try {
                        generateImageEmbedding(bitmap)
                    } catch (e: Exception) {
                        Log.e("EmbeddingGeneration", "Failed to process bitmap", e)
                        null
                    }
                }
            }
        }

        val embeddings = embeddingDeferred.awaitAll().filterNotNull()
        if (embeddings.isEmpty()) {
            throw IllegalStateException("No embeddings could be generated from the provided bitmaps")
        }

        val embeddingLength = embeddings[0].size
        val sumEmbedding = FloatArray(embeddingLength) { 0f }
        for (emb in embeddings) {
            for (i in 0 until embeddingLength) {
                sumEmbedding[i] += emb[i]
            }
        }
        val avgEmbedding = FloatArray(embeddingLength) { i -> sumEmbedding[i] / embeddings.size }
        normalizeL2(avgEmbedding)
    }



    fun closeSession() {
        imageSession?.close()
        textSession?.close()
        imageSession = null
        textSession = null
    }

    private fun getVocab(resources: Resources): Map<String, Int> {
        return hashMapOf<String, Int>().apply {
            resources.openRawResource(R.raw.vocab).use {
                val vocabReader = JsonReader(InputStreamReader(it, "UTF-8"))
                vocabReader.beginObject()
                while (vocabReader.hasNext()) {
                    val key = vocabReader.nextName().replace("</w>", " ")
                    val value = vocabReader.nextInt()
                    put(key, value)
                }
                vocabReader.close()
            }
        }
    }

    private fun getMerges(resources: Resources): HashMap<Pair<String, String>, Int> {
        return hashMapOf<Pair<String, String>, Int>().apply {
            resources.openRawResource(R.raw.merges).use {
                val mergesReader = BufferedReader(InputStreamReader(it))
                mergesReader.useLines { seq ->
                    seq.drop(1).forEachIndexed { i, s ->
                        val list = s.split(" ")
                        val keyTuple = list[0] to list[1].replace("</w>", " ")
                        put(keyTuple, i)
                    }
                }
            }
        }
    }
}
