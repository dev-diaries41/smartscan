package com.fpf.smartscan.workers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ClassificationViewModel(
     application: Application) : AndroidViewModel(application
) {
    private val workManager = WorkManager.getInstance(application)
    private val _organisationActive = MutableStateFlow(false)
    val organisationActive: StateFlow<Boolean> = _organisationActive

    init {
        observeWorkStatus()
    }

    private fun observeWorkStatus() {
        workManager.getWorkInfosByTagLiveData(ClassificationBatchWorker.TAG).observeForever { infos ->
            if (infos.isNullOrEmpty()) {
                _organisationActive.value = false
                return@observeForever
            }

            val runningWorker = infos.firstOrNull { it.state == WorkInfo.State.RUNNING  || it.state == WorkInfo.State.ENQUEUED }
            _organisationActive.value = runningWorker != null
        }
    }
}