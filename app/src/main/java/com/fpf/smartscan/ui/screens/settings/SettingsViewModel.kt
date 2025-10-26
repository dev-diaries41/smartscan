package com.fpf.smartscan.ui.screens.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.fpf.smartscan.data.AppSettings
import com.fpf.smartscan.data.ImportedModel
import com.fpf.smartscan.lib.copyFromUri
import com.fpf.smartscan.lib.deleteModel
import com.fpf.smartscan.lib.exportFile
import com.fpf.smartscan.lib.getImportedModels
import com.fpf.smartscan.lib.importModel
import com.fpf.smartscan.lib.loadSettings
import com.fpf.smartscan.lib.saveSettings
import com.fpf.smartscan.lib.unzipFiles
import com.fpf.smartscan.lib.zipFiles
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeManager
import com.fpf.smartscan.ui.theme.ThemeMode
import com.fpf.smartscansdk.extensions.indexers.ImageIndexer
import com.fpf.smartscansdk.extensions.indexers.VideoIndexer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

class SettingsViewModel(private val application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences(SETTINGS_PREF_NAME, Context.MODE_PRIVATE)
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings

    private val _importedModels = MutableStateFlow(getImportedModels(application))
    val importedModels: StateFlow<List<ImportedModel>> = _importedModels
    private val _event = MutableSharedFlow<String>()
    val event = _event.asSharedFlow()

    companion object {
        private const val SETTINGS_PREF_NAME = "AsyncStorage" // used for backward compatibility with old Storage wrapper which has now been removed (I was original as TypeScript guy)
        private const val TAG = "SettingsViewModel"
        private const val BACKUP_FILENAME = "smartscan_backup.zip"
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

    fun backup(){
        val indexZipFile = File(application.cacheDir, BACKUP_FILENAME)
        val imageIndexFile = File(application.filesDir, ImageIndexer.INDEX_FILENAME)
        val videoIndexFile = File(application.filesDir,  VideoIndexer.INDEX_FILENAME)
        viewModelScope.launch(Dispatchers.IO){
            try {
                zipFiles(indexZipFile, listOf(imageIndexFile, videoIndexFile))
                if(indexZipFile.exists()){
                    exportFile(application, indexZipFile, BACKUP_FILENAME)
                    indexZipFile.delete()
                    _event.emit("Backup successful")
                }
            }catch (e: Exception){
                Log.e(TAG, "Error backing up: ${e.message}")
                _event.emit("Backup error")
            }
        }
    }

    fun restore(uri: Uri){
        val indexZipFile = File(application.cacheDir, BACKUP_FILENAME)

        viewModelScope.launch(Dispatchers.IO){
            try {
                copyFromUri(application, uri, indexZipFile)
                if(indexZipFile.exists()){
                    unzipFiles(indexZipFile, application.filesDir)
                    indexZipFile.delete()
                    _event.emit("Restore successful")
                }
            }catch (e: Exception){
                Log.e(TAG, "Error restoring: ${e.message}")
                _event.emit("Restore error")
            }
        }
    }
}
