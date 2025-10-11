package com.fpf.smartscan.services

import android.app.Application
import android.content.Intent
import com.fpf.smartscan.lib.isServiceRunning

fun startIndexing(application: Application, mediaType: String) {
    Intent(application, MediaIndexForegroundService::class.java)
        .putExtra(MediaIndexForegroundService.EXTRA_MEDIA_TYPE, mediaType)
        .also { intent -> application.startForegroundService(intent) }
}

fun refreshIndex(application: Application, mediaType: String) {
    val running = isServiceRunning(application, MediaIndexForegroundService::class.java)
    if(running){
        application.stopService(Intent(application, MediaIndexForegroundService::class.java))
    }
    startIndexing(application, mediaType)
}