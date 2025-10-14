package com.fpf.smartscan

import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.fpf.smartscan.lib.isServiceRunning
import com.fpf.smartscan.lib.loadSettings
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.services.startIndexing
import com.fpf.smartscan.ui.permissions.StorageAccess
import com.fpf.smartscan.ui.permissions.getStorageAccess
import com.fpf.smartscan.ui.theme.MyAppTheme
import com.fpf.smartscan.ui.theme.ThemeManager
import com.fpf.smartscansdk.extensions.indexers.ImageIndexer
import com.fpf.smartscansdk.extensions.indexers.VideoIndexer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val sharedPrefs by lazy { application.getSharedPreferences("AsyncStorage", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appSettings = loadSettings(sharedPrefs)
        ThemeManager.updateColorScheme(appSettings.color)
        ThemeManager.updateThemeMode(appSettings.theme)

        createNotificationChannel(
            channelId = getString(R.string.worker_channel_id),
            channelName = getString(R.string.worker_channel_name),
            description = getString(R.string.worker_channel_description)
        )
        updateEdgeToEdge()

        lifecycleScope.launch {
            ThemeManager.themeMode.collectLatest {
                updateEdgeToEdge()
            }
        }

        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .crossfade(true)
                .memoryCache { MemoryCache.Builder().maxSizePercent(this, 0.25).build() }
                .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizePercent(0.02).build() }
                .build()
        }

    setContent {
            MyAppTheme {
                MainScreen()
            }
        }
    }

    override fun onResume(){
        super.onResume()
        val appSettings = loadSettings(sharedPrefs)
        val lastIndexed = sharedPrefs.getString("lastIndexed", null)?.toLongOrNull() // getString used for backward-compat
        if(lastIndexed == null) return

        val now = System.currentTimeMillis()
        val shouldIndex = when (appSettings.indexFrequency) {
            "1 Day" -> (now - lastIndexed) > TimeUnit.DAYS.toMillis(1)
            "1 Week" -> (now - lastIndexed) > TimeUnit.DAYS.toMillis(7)
            else -> false
        }

        if (shouldIndex) {
            if(isServiceRunning(application, MediaIndexForegroundService::class.java)) return // additional check to prevent service running again if condition met

            val imageIndexFile = File(application.filesDir, ImageIndexer.INDEX_FILENAME)
            val videoIndexFile = File(application.filesDir, VideoIndexer.INDEX_FILENAME)
            if(!imageIndexFile.exists() || !videoIndexFile.exists()) return // prevent full re-index due to migration incomplete

            val permissionType = getStorageAccess(application)
            if(permissionType == StorageAccess.Denied) return
            startIndexing(application, MediaIndexForegroundService.TYPE_BOTH)
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String, description: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH
        ).apply { this.description = description }
        notificationManager.createNotificationChannel(channel)
    }
    private fun updateEdgeToEdge() {
        val isDarkTheme = ThemeManager.isDarkTheme(resources)
        enableEdgeToEdge(
            statusBarStyle = if (isDarkTheme) SystemBarStyle.dark(Color.Transparent.toArgb()) else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Black.toArgb()),
            navigationBarStyle = if (isDarkTheme) SystemBarStyle.dark(Color.Transparent.toArgb()) else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Black.toArgb())
        )
    }

}
