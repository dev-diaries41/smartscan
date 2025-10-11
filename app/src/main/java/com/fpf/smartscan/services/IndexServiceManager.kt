package com.fpf.smartscan.services

import android.content.Context
import android.content.Intent
import com.fpf.smartscan.lib.isServiceRunning

fun startIndexing(context: Context, mediaType: String) {
    Intent(context.applicationContext, MediaIndexForegroundService::class.java)
        .putExtra(MediaIndexForegroundService.EXTRA_MEDIA_TYPE, mediaType)
        .also { intent -> context.applicationContext.startForegroundService(intent) }
}

fun refreshIndex(context: Context, mediaType: String) {
    val running = isServiceRunning(context.applicationContext, MediaIndexForegroundService::class.java)
    if(running){
        context.applicationContext.stopService(Intent(context.applicationContext, MediaIndexForegroundService::class.java))
    }
    startIndexing(context.applicationContext, mediaType)
}