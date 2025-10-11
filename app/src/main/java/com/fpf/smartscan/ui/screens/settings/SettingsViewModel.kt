package com.fpf.smartscan.ui.screens.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.workers.scheduleClassificationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.fpf.smartscan.R
import com.fpf.smartscan.data.AppSettings
import com.fpf.smartscan.data.ImportedModel
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingEntity
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingDatabase
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingRepository
import com.fpf.smartscan.lib.cancelWorker
import com.fpf.smartscan.lib.deleteModel
import com.fpf.smartscan.lib.fetchBitmapsFromDirectory
import com.fpf.smartscan.lib.getImportedModels
import com.fpf.smartscan.lib.importModel
import com.fpf.smartscan.lib.isServiceRunning
import com.fpf.smartscan.lib.isWorkScheduled
import com.fpf.smartscan.lib.loadSettings
import com.fpf.smartscan.lib.saveSettings
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class SettingsViewModel(private val application: Application) : AndroidViewModel(application) {
    private val repository: PrototypeEmbeddingRepository = PrototypeEmbeddingRepository(PrototypeEmbeddingDatabase.getDatabase(application).prototypeEmbeddingDao())
    val prototypeList: StateFlow<List<PrototypeEmbeddingEntity>> = repository.allEmbeddings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val sharedPrefs = application.getSharedPreferences(SETTINGS_PREF_NAME, Context.MODE_PRIVATE)
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings

    private val _importedModels = MutableStateFlow(getImportedModels(application))
    val importedModels: StateFlow<List<ImportedModel>> = _importedModels
    private val _importEvent = MutableSharedFlow<String>()
    val importEvent = _importEvent.asSharedFlow()
    private var updateJob: Job? = null

    companion object {
        private const val SETTINGS_PREF_NAME = "AsyncStorage" // used for backward compatibility with old Storage wrapper which has now been removed (I was original as TypeScript guy)
        private const val TAG = "SettingsViewModel"
    }

    init {
        _appSettings.value = loadSettings(sharedPrefs)
    }

    fun updateEnableScan(enable: Boolean){
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(enableScan = enable)
        saveSettings(sharedPrefs, _appSettings.value)
        if(enable){
            updateWorker()
        }else{
            viewModelScope.launch {
                val workScheduled = isWorkScheduled(getApplication(), ClassificationWorker.TAG )
                if(workScheduled){
                    cancelWorker(application, uniqueWorkName = ClassificationWorker.TAG, tag = ClassificationBatchWorker.TAG)
                }
            }
        }
    }

    fun updateFrequency(frequency: String) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(frequency = frequency)
        saveSettings(sharedPrefs, _appSettings.value)
        updateWorker()
    }

    fun updateIndexFrequency(frequency: String) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(indexFrequency = frequency)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun addTargetDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.targetDirectories + dir
        _appSettings.value = currentSettings.copy(targetDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun deleteTargetDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.targetDirectories - dir
        _appSettings.value = currentSettings.copy(targetDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
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
                saveSettings(sharedPrefs, _appSettings.value)
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
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateSimilarityThreshold(threshold: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(similarityThreshold = threshold)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateOrganiserSimilarityThreshold(threshold: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(organiserSimilarityThreshold = threshold)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateOrganiserConfidenceMargin(margin: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(organiserConfMargin = margin)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateNumberSimilarImages(numberSimilarResults: String) {
        val number = numberSimilarResults.toIntOrNull()?.takeIf { it in 1..20 } ?: 5
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(numberSimilarResults = number)
        saveSettings(sharedPrefs, _appSettings.value)
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
    fun onSettingsDetailsExit(initialDestinationDirectories: List<String>, initialTargetDirectories: List<String>, initialOrganiserSimilarity: Float, initialOrganiserConfMargin: Float) {
        val destinationChanged = initialDestinationDirectories != _appSettings.value.destinationDirectories
        val targetChanged = initialTargetDirectories != _appSettings.value.targetDirectories
        val organiserSimChanged = initialOrganiserSimilarity != _appSettings.value.organiserSimilarityThreshold
        val organiserConfMarginChanged = initialOrganiserConfMargin != _appSettings.value.organiserConfMargin

        val shouldUpdateWorker = listOf(destinationChanged, targetChanged, organiserSimChanged, organiserConfMarginChanged).any { it }

        viewModelScope.launch {
            if (destinationChanged) {
                updateJob?.cancelAndJoin()
                updateJob = updatePrototypes(getApplication(), _appSettings.value.destinationDirectories)
                updateJob?.join()
                updateJob = null
            }

            if (shouldUpdateWorker) {
                updateWorker()
            }
        }
    }

    fun onImportModel( uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                importModel(application, uri)
                _importedModels.value = getImportedModels(application)
                _importEvent.emit("Model imported successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error importing model: ${e.message}")
                _importEvent.emit("Error importing model")
            }
        }
    }

    fun onDeleteModel(model: ImportedModel){
        if(deleteModel(application, model)) _importedModels.value = _importedModels.value - model
    }
}
