package com.fpf.smartscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.fpf.smartscan.lib.Storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel( application: Application) : AndroidViewModel(application) {

    companion object {
        private const val UPDATES_KEY = "UPDATES_KEY"
    }
    val storage = Storage.getInstance(getApplication())

    val versionName: String? = try {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        packageInfo.versionName
    } catch (e: Exception) {
        null
    }

    private val _isUpdatePopUpVisible = MutableStateFlow(!hasShownUpdatePopUp)
    val  isUpdatePopUpVisible: StateFlow<Boolean> = _isUpdatePopUpVisible

    val hasShownUpdatePopUp: Boolean
        get() = storage.getItem(UPDATES_KEY) == versionName

    fun closeUpdatePopUp(){
        _isUpdatePopUpVisible.value = false
        storage.setItem(UPDATES_KEY, versionName.toString())
    }

    fun getUpdates(): List<String>{
        return listOf(
            application.getString(R.string.update_copy_to_clipboard),
            application.getString(R.string.update_reddit)
        )
    }
}