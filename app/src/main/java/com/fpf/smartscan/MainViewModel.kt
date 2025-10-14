package com.fpf.smartscan

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel( application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "AsyncStorage"
        private const val UPDATES_KEY = "UPDATES_KEY"
    }
    private val sharedPrefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val versionName: String? = try {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        packageInfo.versionName
    } catch (e: Exception) {
        null
    }

    private val _isUpdatePopUpVisible = MutableStateFlow(!hasShownUpdatePopUp)
    val  isUpdatePopUpVisible: StateFlow<Boolean> = _isUpdatePopUpVisible

    val hasShownUpdatePopUp: Boolean
        get() = sharedPrefs.getString(UPDATES_KEY, null) == versionName

    fun closeUpdatePopUp(){
        _isUpdatePopUpVisible.value = false
        sharedPrefs.edit { putString(UPDATES_KEY, versionName.toString()) }
    }

    fun getUpdates(): List<String>{
        return listOf(
            application.getString(R.string.update_image_to_image_search),
            application.getString(R.string.update_select_searchable_folders),
            application.getString(R.string.update_increase_search_limit),
            application.getString(R.string.update_theme_settings),
            application.getString(R.string.update_new_results_layout),
            application.getString(R.string.update_auto_organisation_fix),
            application.getString(R.string.update_auto_organisation_notice)

        )
    }
}