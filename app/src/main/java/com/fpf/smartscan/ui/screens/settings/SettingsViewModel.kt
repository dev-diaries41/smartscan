package com.fpf.smartscan.ui.screens.settings

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fpf.smartscan.data.prototypes.PrototypeEmbedding
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingDatabase
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.fetchBitmapsFromDirectory
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.workers.ClassificationBatchWorker
import com.fpf.smartscan.workers.ClassificationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlin.collections.any
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Serializable
data class AppSettings(
    val enableScan: Boolean = false,
    val targetDirectories: List<String> = emptyList(),
    val frequency: String = "1 Day",
    val destinationDirectories: List<String> = emptyList(),
    val similarityThreshold: Float = 0.2f,
    val numberSimilarResults: Int = 5,
    val indexFrequency: String = "1 Week",
    )

class SettingsViewModel(private val application: Application) : AndroidViewModel(application) {
    private val repository: PrototypeEmbeddingRepository =
        PrototypeEmbeddingRepository(PrototypeEmbeddingDatabase.getDatabase(application).prototypeEmbeddingDao())
    val prototypeList: LiveData<List<PrototypeEmbedding>> = repository.allEmbeddings
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

    fun updateTargetDirectories(directories: List<String>) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(targetDirectories = directories)
        saveSettings()
    }

    fun updateDestinationDirectories(directories: List<String>) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(destinationDirectories = directories)
        saveSettings()
    }

    fun updateSimilarityThreshold(threshold: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(similarityThreshold = threshold)
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

            val embeddingsHandler = Embeddings(context.resources, ModelType.IMAGE)
            val semaphore = Semaphore(1)

            try {
                val jobs = missingUris.map { uri ->
                    async {
                        semaphore.withPermit {
                            try {
                                val bitmaps = fetchBitmapsFromDirectory(context, uri.toUri(), 30)
                                val prototypeEmbedding = embeddingsHandler.generatePrototypeEmbedding(context, bitmaps)
                                repository.insert(
                                    PrototypeEmbedding(
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

    fun isServiceRunning(context: Context, serviceClass: Class<out Service>): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return am.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == serviceClass.name
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
                scheduleClassificationWorker(getApplication(), uriArray as Array<Uri?>, _appSettings.value.frequency, delayInHour)
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


    fun onSettingsDetailsExit(initialDestinationDirectories: List<String>, initialTargetDirectories: List<String>) {
        val destinationChanged = initialDestinationDirectories != _appSettings.value.destinationDirectories
        val targetChanged = initialTargetDirectories != _appSettings.value.targetDirectories

        viewModelScope.launch {
            if (destinationChanged) {
                val job = updatePrototypes(getApplication(), _appSettings.value.destinationDirectories)
                job.join()
            }

            if (destinationChanged || targetChanged) {
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


    fun verifyDir(uri: Uri, context: Context): Boolean {
        val documentFile = DocumentFile.fromTreeUri(context, uri)
        if (documentFile == null || !documentFile.isDirectory) {
            Toast.makeText(context, "Invalid directory", Toast.LENGTH_SHORT).show()
            return false
        }

        val imageCount = documentFile.listFiles().count { file ->
            file.type?.startsWith("image/") == true
        }

        if (imageCount < 10) {
            Toast.makeText(context, "Directory must contain at least 10 images", Toast.LENGTH_LONG).show()
            return false
        }
        return true
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

    private suspend fun isWorkScheduled(context: Context, workName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val workManager = WorkManager.getInstance(context)
            val workInfoList = workManager.getWorkInfosForUniqueWork(workName).get()
            workInfoList.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
    }
}
