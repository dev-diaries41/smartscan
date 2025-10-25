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
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingEntity
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingDatabase
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingRepository
import com.fpf.smartscan.lib.deleteModel
import com.fpf.smartscan.lib.getImportedModels
import com.fpf.smartscan.lib.importModel
import com.fpf.smartscan.lib.loadSettings
import com.fpf.smartscan.lib.saveSettings
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeManager
import com.fpf.smartscan.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn

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

    companion object {
        private const val SETTINGS_PREF_NAME = "AsyncStorage" // used for backward compatibility with old Storage wrapper which has now been removed (I was original as TypeScript guy)
        private const val TAG = "SettingsViewModel"
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
                _importEvent.emit("Model imported successfully")
            } catch (e: Exception) {
                val defaultErrorMessage = "Error importing model"
                val invalidFileError = "Invalid model file"
                Log.e(TAG, "$defaultErrorMessage: ${e.message}")
                val errorMessage = if(e.message == invalidFileError) invalidFileError else defaultErrorMessage
                _importEvent.emit(errorMessage)
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
}
