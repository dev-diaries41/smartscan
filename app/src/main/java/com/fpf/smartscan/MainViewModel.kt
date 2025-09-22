package com.fpf.smartscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.fpf.smartscan.lib.Storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel( application: Application) : AndroidViewModel(application) {

    companion object {
        const val UPDATES_KEY = "UPDATES_KEY"
    }
    private val _isUpdatePopUpVisible = MutableStateFlow(false)
    val  isUpdatePopUpVisible: StateFlow<Boolean> = _isUpdatePopUpVisible

    val storage = Storage.getInstance(application)

    fun showUpdatePopUp(){
        _isUpdatePopUpVisible.value = true
    }

    fun closeUpdatePopUp(){
        _isUpdatePopUpVisible.value = false
        storage.setItem(UPDATES_KEY, true.toString())
    }

    fun getUpdates(): List<String>{
        return listOf(
            application.getString(R.string.update_copy_to_clipboard),
            application.getString(R.string.update_reddit, application.getString(R.string.reddit_url),
        ))
    }
}