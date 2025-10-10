package com.fpf.smartscan.ui.screens.settings

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.lib.Storage
import com.fpf.smartscan.workers.scheduleClassificationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import androidx.core.net.toUri
import androidx.work.WorkManager
import com.fpf.smartscan.R
import com.fpf.smartscan.data.AppSettings
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingEntity
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingDatabase
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingRepository
import com.fpf.smartscan.lib.fetchBitmapsFromDirectory
import com.fpf.smartscan.lib.isServiceRunning
import com.fpf.smartscan.lib.isWorkScheduled
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.workers.ClassificationBatchWorker
import com.fpf.smartscan.workers.ClassificationWorker
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.ml.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.ml.models.ResourceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlin.collections.any
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class SettingsViewModel(private val application: Application) : AndroidViewModel(application) {
    private val repository: PrototypeEmbeddingRepository = PrototypeEmbeddingRepository(PrototypeEmbeddingDatabase.getDatabase(application).prototypeEmbeddingDao())
    val prototypeList: StateFlow<List<PrototypeEmbeddingEntity>> = repository.allEmbeddings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val storage = Storage.getInstance(getApplication())
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    init {
        loadSettings()
    }

    fun updateEnableScan(enable: Boolean){
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(enableScan = enable)
        saveSettings()
        if(enable){
            updateWorker()
        }else{
            viewModelScope.launch {
                val workScheduled = isWorkScheduled(getApplication(), ClassificationWorker.TAG )
                if(workScheduled){
                    cancelClassificationWorker()
                }
            }
        }
    }

    fun updateFrequency(frequency: String) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(frequency = frequency)
        saveSettings()
        updateWorker()
    }

    fun updateIndexFrequency(frequency: String) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(indexFrequency = frequency)
        saveSettings()
    }

    fun addTargetDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.targetDirectories + dir
        _appSettings.value = currentSettings.copy(targetDirectories = newDirs)
        saveSettings()
    }

    fun deleteTargetDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.targetDirectories - dir
        _appSettings.value = currentSettings.copy(targetDirectories = newDirs)
        saveSettings()
    }


    fun addDestinationDirectory(dirString: String) {
        val uri = dirString.toUri()

        viewModelScope.launch {
            try {
                val hasTenImages = withContext(Dispatchers.IO) {
                    val docId = DocumentsContract.getTreeDocumentId(uri)
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)
                    val resolver = application.contentResolver

                    val projection = arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                        var count = 0
                        while (cursor.moveToNext()) {
                            val mime = cursor.getString(0) ?: continue
                            if (mime.startsWith("image/") && ++count >= 10) {
                                return@withContext true
                            }
                        }
                        return@withContext false
                    } ?: false
                }

                if (!hasTenImages) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            application,
                            "Directory must contain at least 10 images",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val current = _appSettings.value
                val newList = current.destinationDirectories + dirString
                _appSettings.value = current.copy(destinationDirectories = newList)
                saveSettings()
            }catch (e: Exception){
                Log.e("addDestinationDir", "unexpected error", e)
                Toast.makeText(application, "Unexpected error", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun deleteDestinationDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.destinationDirectories - dir
        _appSettings.value = currentSettings.copy(destinationDirectories = newDirs)
        saveSettings()
    }

    fun updateSimilarityThreshold(threshold: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(similarityThreshold = threshold)
        saveSettings()
    }

    fun updateOrganiserSimilarityThreshold(threshold: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(organiserSimilarityThreshold = threshold)
        saveSettings()
    }

    fun updateOrganiserConfidenceMargin(margin: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(organiserConfMargin = margin)
        saveSettings()
    }

    fun updateNumberSimilarImages(numberSimilarResults: String) {
        val number = numberSimilarResults.toIntOrNull()?.takeIf { it in 1..20 } ?: 5
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(numberSimilarResults = number)
        saveSettings()
    }

    fun updatePrototypes(context: Context, uris: List<String>): Job {
        if (uris.isEmpty()) return Job().apply { complete() }
        return viewModelScope.launch(Dispatchers.IO) {
            val existingEmbeddings = repository.getAllEmbeddingsSync()
            val existingIds = existingEmbeddings.map { it.id }.toSet()
            val missingUris = uris.filterNot { it in existingIds }
            val extraEmbeddings = existingEmbeddings.filter { it.id !in uris }

            extraEmbeddings.forEach { prototype ->
                try {
                    repository.delete(prototype)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete prototype for URI: ${prototype.id}", e)
                }
            }

            if (missingUris.isEmpty()) return@launch

            val embeddingsHandler = ClipImageEmbedder(context.resources, ResourceId(R.raw.image_encoder_quant_int8))
            embeddingsHandler.initialize()

            val semaphore = Semaphore(1)

            try {
                val jobs = missingUris.map { uri ->
                    async {
                        semaphore.withPermit {
                            try {
                                val bitmaps = fetchBitmapsFromDirectory(context, uri.toUri(), 30)
                                val rawEmbeddings = embeddingsHandler.embedBatch(context, bitmaps)
                                val prototypeEmbedding = generatePrototypeEmbedding(rawEmbeddings)
                                repository.insert(
                                    PrototypeEmbeddingEntity(
                                        id = uri,
                                        date = System.currentTimeMillis(),
                                        embeddings = prototypeEmbedding
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to generate or insert prototype for URI: $uri", e)
                            }
                        }
                    }
                }
                jobs.awaitAll()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during prototype update", e)
            } finally {
                embeddingsHandler.closeSession()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun updateWorker() {
        if (_appSettings.value.targetDirectories.isNotEmpty() &&
            _appSettings.value.enableScan &&
            _appSettings.value.frequency.isNotEmpty() &&
            _appSettings.value.destinationDirectories.isNotEmpty()) {

            val uriArray = _appSettings.value.targetDirectories.map { it.toUri() }.toTypedArray()
            viewModelScope.launch {
                val indexingInProgress = isServiceRunning(application, MediaIndexForegroundService::class.java)
                // This delay prevents indexing and classification workers running at the same time to limit resource usage.
                // Big 12 hour buffer used to account for image AND video indexing.
                val delayInHour = if (indexingInProgress) 12L else null
                scheduleClassificationWorker(getApplication(), uriArray as Array<Uri?>, _appSettings.value.frequency, confidenceMargin = _appSettings.value.organiserConfMargin, similarityThreshold = _appSettings.value.organiserSimilarityThreshold, delayInHours = delayInHour)
            }
        }
    }


    @SuppressLint("ImplicitSamInstance")
    fun refreshImageIndex() {
        viewModelScope.launch {
            val running = isServiceRunning(application, MediaIndexForegroundService::class.java)
            if(running){
                getApplication<Application>().stopService(Intent(getApplication<Application>(),
                    MediaIndexForegroundService::class.java))
            }
            startImageIndexing()
        }
    }

    @SuppressLint("ImplicitSamInstance")
    fun refreshVideoIndex() {
        viewModelScope.launch {
            val running = isServiceRunning(application, MediaIndexForegroundService::class.java)
            if(running){
                getApplication<Application>().stopService(Intent(getApplication<Application>(),
                    MediaIndexForegroundService::class.java))
            }
            startVideoIndexing()
        }
    }


    fun onSettingsDetailsExit(initialDestinationDirectories: List<String>, initialTargetDirectories: List<String>, initialOrganiserSimilarity: Float, initialOrganiserConfMargin: Float) {
        val destinationChanged = initialDestinationDirectories != _appSettings.value.destinationDirectories
        val targetChanged = initialTargetDirectories != _appSettings.value.targetDirectories
        val organiserSimChanged = initialOrganiserSimilarity != _appSettings.value.organiserSimilarityThreshold
        val organiserConfMarginChanged = initialOrganiserConfMargin != _appSettings.value.organiserConfMargin

        val shouldUpdateWorker = listOf(destinationChanged, targetChanged, organiserSimChanged, organiserConfMarginChanged).any { it }

        viewModelScope.launch {
            if (destinationChanged) {
                val job = updatePrototypes(getApplication(), _appSettings.value.destinationDirectories)
                job.join()
            }

            if (shouldUpdateWorker) {
                updateWorker()
            }
        }
    }

    private fun startImageIndexing() {
        Intent(application, MediaIndexForegroundService::class.java)
            .putExtra(
                MediaIndexForegroundService.EXTRA_MEDIA_TYPE,
                MediaIndexForegroundService.TYPE_IMAGE
            ).also { intent ->
                application.startForegroundService(intent)
            }
    }

    private fun startVideoIndexing() {
        Intent(application, MediaIndexForegroundService::class.java)
            .putExtra(
                MediaIndexForegroundService.EXTRA_MEDIA_TYPE,
                MediaIndexForegroundService.TYPE_VIDEO
            ).also { intent ->
                application.startForegroundService(intent)
            }
    }

    private fun cancelClassificationWorker(){
        val workManager = WorkManager.getInstance(getApplication())
        workManager.cancelUniqueWork(ClassificationWorker.TAG)
        workManager.cancelAllWorkByTag(ClassificationBatchWorker.TAG)
    }

    private fun loadSettings() {
        val jsonSettings = storage.getItem("app_settings")
        _appSettings.value = if (jsonSettings != null) {
            try {
                Json.decodeFromString<AppSettings>(jsonSettings)
            } catch (e: Exception) {
                Log.e("Settings", "Failed to decode settings", e)
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }

    private fun saveSettings() {
        val jsonSettings = Json.encodeToString(_appSettings.value)
        storage.setItem("app_settings", jsonSettings)
    }

}
