package com.fpf.smartscan.ui.screens.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.fpf.smartscan.data.AppSettings
import com.fpf.smartscan.data.ImportedModel
import com.fpf.smartscan.lib.ImageIndexListener
import com.fpf.smartscan.lib.VideoIndexListener
import com.fpf.smartscan.lib.copyFromUri
import com.fpf.smartscan.lib.copyToUri
import com.fpf.smartscan.lib.deleteModel
import com.fpf.smartscan.lib.getImportedModels
import com.fpf.smartscan.lib.hashFile
import com.fpf.smartscan.lib.importModel
import com.fpf.smartscan.lib.loadSettings
import com.fpf.smartscan.lib.saveSettings
import com.fpf.smartscan.lib.unzipFiles
import com.fpf.smartscan.lib.zipFiles
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeManager
import com.fpf.smartscan.ui.theme.ThemeMode
import com.fpf.smartscansdk.core.processors.Metrics
import com.fpf.smartscansdk.extensions.indexers.ImageIndexer
import com.fpf.smartscansdk.extensions.indexers.VideoIndexer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

class SettingsViewModel(private val application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings

    private val _importedModels = MutableStateFlow(getImportedModels(application))
    val importedModels: StateFlow<List<ImportedModel>> = _importedModels
    private val _event = MutableSharedFlow<String>()
    val event = _event.asSharedFlow()

    private val _isBackupLoading = MutableStateFlow(false)
    val isBackupLoading: StateFlow<Boolean> = _isBackupLoading

    private val _isRestoreLoading = MutableStateFlow(false)
    val isRestoreLoading: StateFlow<Boolean> = _isRestoreLoading



    companion object {
        private const val PREF_NAME = "AsyncStorage" // used for backward compatibility with old Storage wrapper which has now been removed (I was original as TypeScript guy)
        private const val TAG = "SettingsViewModel"
        const val BACKUP_FILENAME = "smartscan_backup.zip"
        private const val HASH_FILENAME = "hash.txt"
    }

    init {
        _appSettings.value = loadSettings(sharedPrefs)
    }

    fun updateIndexFrequency(frequency: String) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(indexFrequency = frequency)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateSimilarityThreshold(threshold: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(similarityThreshold = threshold)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun onImportModel( uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                importModel(application, uri)
                _importedModels.value = getImportedModels(application)
                _event.emit("Model imported successfully")
            } catch (e: Exception) {
                val defaultErrorMessage = "Error importing model"
                val invalidFileError = "Invalid model file"
                Log.e(TAG, "$defaultErrorMessage: ${e.message}")
                val errorMessage = if(e.message == invalidFileError) invalidFileError else defaultErrorMessage
                _event.emit(errorMessage)
            }
        }
    }

    fun onDeleteModel(model: ImportedModel){
        if(deleteModel(application, model)) _importedModels.value = _importedModels.value - model
    }

    fun addSearchableImageDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableImageDirectories + dir
        _appSettings.value = currentSettings.copy(searchableImageDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun deleteSearchableImageDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableImageDirectories - dir
        _appSettings.value = currentSettings.copy(searchableImageDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }
    fun addSearchableVideoDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableVideoDirectories + dir
        _appSettings.value = currentSettings.copy(searchableVideoDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun deleteSearchableVideoDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableVideoDirectories - dir
        _appSettings.value = currentSettings.copy(searchableVideoDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateTheme(theme: ThemeMode){
        ThemeManager.updateThemeMode(theme)
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(theme = theme)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateColorScheme(colorScheme: ColorSchemeType){
        ThemeManager.updateColorScheme(colorScheme)
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(color = colorScheme)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun backup(uri: Uri){
        val indexZipFile = File(application.cacheDir, BACKUP_FILENAME)
        val imageIndexFile = File(application.filesDir, ImageIndexer.INDEX_FILENAME)
        val videoIndexFile = File(application.filesDir,  VideoIndexer.INDEX_FILENAME)
        val hashFile = File(application.cacheDir, HASH_FILENAME)
        val filesToZip = listOf(imageIndexFile, videoIndexFile, hashFile)
        _isBackupLoading.value = true

        viewModelScope.launch(Dispatchers.IO){
            try {
                val hashes: List<String> = listOf(imageIndexFile, videoIndexFile).filter { it.exists() }.map{hashFile(it)}
                hashFile.writeText(hashes.joinToString("\n") )

                zipFiles(indexZipFile, filesToZip)
                copyToUri(application, uri, indexZipFile)
                _event.emit("Backup successful")
            }catch (e: Exception){
                Log.e(TAG, "Error backing up: ${e.message}")
                _event.emit("Backup error")
            }finally {
                indexZipFile.delete()
                hashFile.delete()
                _isBackupLoading.emit(false)
            }
        }
    }

    fun restore(uri: Uri){
        val indexZipFile = File(application.cacheDir, BACKUP_FILENAME)
        _isRestoreLoading.value = true

        viewModelScope.launch(Dispatchers.IO){
            try {
                copyFromUri(application, uri, indexZipFile)
                val extractedFiles = unzipFiles(indexZipFile, application.filesDir)
                if(!isValidBackupFile((extractedFiles))){
                    extractedFiles.forEach { it.delete() }
                    error("Invalid backup file")
                }
                _event.emit("Restore successful")
                ImageIndexListener.onComplete(application, Metrics.Success()) // call onComplete to trigger refresh in search screen
                VideoIndexListener.onComplete(application, Metrics.Success())
                sharedPrefs.edit { putString("lastIndexed", System.currentTimeMillis().toString()) } // so scheduling can be triggered
            }catch (e: Exception){
                Log.e(TAG, "Error restoring: ${e.message}")
                _event.emit("Restore error")
            }finally {
                indexZipFile.delete()
                _isRestoreLoading.emit(false)
            }
        }
    }

    private suspend fun isValidBackupFile(extractedFiles: List<File>): Boolean{
        val hashFile = extractedFiles.find { it.name == HASH_FILENAME }?: return false
        val hashesFromFile: List<String> = hashFile.readLines()
        if(hashesFromFile.isEmpty()) return false // must have hash for at least 1 of image or video index file

        val indexFiles = extractedFiles.filterNot{it.name == HASH_FILENAME}
        val indexHashes = indexFiles.map{hashFile(it)}
        return hashesFromFile.toSet() == indexHashes.toSet()
    }
}
