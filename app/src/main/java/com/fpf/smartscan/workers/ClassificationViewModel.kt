package com.fpf.smartscan.workers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager

class ClassificationViewModel(
     application: Application) : AndroidViewModel(application
) {
    private val workManager = WorkManager.getInstance(application)
    private val _organisationActive = MutableLiveData(false)
    val organisationActive: LiveData<Boolean> = _organisationActive

    init {
        observeWorkStatus()
    }

    private fun observeWorkStatus() {
        workManager.getWorkInfosByTagLiveData(ClassificationBatchWorker.TAG).observeForever { infos ->
            if (infos.isNullOrEmpty()) {
                _organisationActive.postValue(false)
                return@observeForever
            }

            val runningWorker = infos.firstOrNull { it.state == WorkInfo.State.RUNNING  || it.state == WorkInfo.State.ENQUEUED }
            if (runningWorker != null) {
                _organisationActive.postValue(true)

            } else {
                _organisationActive.postValue(false)
            }
        }
    }
}