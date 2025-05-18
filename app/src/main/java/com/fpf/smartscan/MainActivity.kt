package com.fpf.smartscan

import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fpf.smartscan.lib.Storage
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.ui.permissions.StorageAccess
import com.fpf.smartscan.ui.permissions.getStorageAccess
import com.fpf.smartscan.ui.screens.settings.AppSettings
import com.fpf.smartscan.ui.theme.MyAppTheme
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel(
            channelId = getString(R.string.worker_channel_id),
            channelName = getString(R.string.worker_channel_name),
            description = getString(R.string.worker_channel_description)
        )
        enableEdgeToEdge()

    setContent {
            MyAppTheme {
                MainScreen()
            }
        }
    }

    override fun onResume(){
        super.onResume()
        val storage = Storage.getInstance(applicationContext)
        val jsonSettings = storage.getItem("app_settings")
        val appSettings = if (jsonSettings != null) {
            try {
                Json.decodeFromString<AppSettings>(jsonSettings)
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }

        val lastIndexed = storage.getItem("lastIndexed")?.toLongOrNull()
        if(lastIndexed == null) return

        val now = System.currentTimeMillis()

        val shouldIndex = when (appSettings.indexFrequency) {
            "1 Day" -> (now - lastIndexed) > TimeUnit.DAYS.toMillis(1)
            "1 Week" -> (now - lastIndexed) > TimeUnit.DAYS.toMillis(7)
            else -> false
        }

        if (shouldIndex) {
            val permissionType = getStorageAccess(application)
            if(permissionType == StorageAccess.Denied) return
            Intent(application, MediaIndexForegroundService::class.java)
                .putExtra(
                    MediaIndexForegroundService.EXTRA_MEDIA_TYPE,
                    MediaIndexForegroundService.TYPE_BOTH
                ).also { intent ->
                    application.startForegroundService(intent)
                }
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String, description: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            this.description = description
        }
        notificationManager.createNotificationChannel(channel)
    }
}
