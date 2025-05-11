package com.fpf.smartscan.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fpf.smartscan.R
import com.fpf.smartscan.MainActivity
import com.fpf.smartscan.lib.processors.IVideoIndexListener
import com.fpf.smartscan.lib.processors.VideoIndexer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object VideoIndexRepository : IVideoIndexListener {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    override fun onProgress(processedCount: Int, total: Int) {
        val currentProgress = processedCount.toFloat() / total.toFloat()
        if(currentProgress > 0f){
            _progress.value = currentProgress
        }
    }
}

class VideoIndexForegroundService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 102
    }

    var videoIndexer: VideoIndexer? = null
    private val listener: IVideoIndexListener = VideoIndexRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
        videoIndexer = VideoIndexer(application, listener)
    }

    private fun startForegroundServiceNotification() {
        val activityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "video_foreground_service")
            .setContentTitle("Video Foreground Service")
            .setContentText("Indexing videos")
            .setSmallIcon(R.drawable.smartscan_logo)
            .setContentIntent(activityPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "video_foreground_service",
            "Video Foreground Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ids = queryAllVideoIds()
        serviceScope.launch {
            try {
                videoIndexer?.indexVideos(ids)
            } catch (e: CancellationException) {
            } catch (t: Throwable) {
                Log.e("VideoIndexService", "Indexing failed", t)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        videoIndexer?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun queryAllVideoIds(): List<Long> {
        val videoIds = mutableListOf<Long>()
        val projection = arrayOf(MediaStore.Video.Media._ID)
        // You can also sort by DATE_ADDED or DATE_TAKEN if you prefer
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        applicationContext.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            while (cursor.moveToNext()) {
                videoIds.add(cursor.getLong(idColumn))
            }
        }
        return videoIds
    }

}