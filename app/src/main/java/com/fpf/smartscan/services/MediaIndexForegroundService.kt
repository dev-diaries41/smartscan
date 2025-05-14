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
import com.fpf.smartscan.lib.Storage
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.processors.IIndexListener
import com.fpf.smartscan.lib.processors.ImageIndexListener
import com.fpf.smartscan.lib.processors.ImageIndexer
import com.fpf.smartscan.lib.processors.VideoIndexListener
import com.fpf.smartscan.lib.processors.VideoIndexer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediaIndexForegroundService : Service() {
    companion object {
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
        const val TYPE_BOTH = "both"
        private const val NOTIFICATION_ID = 200
        private const val TAG = "MediaIndexService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)

    private var embeddingHandler: Embeddings? = null
    private lateinit var listener: IIndexListener

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
    }

    private fun startForegroundServiceNotification() {
        val activityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this, getString(R.string.service_media_index_channel_id)
        )
            .setContentTitle(getString(R.string.notif_title_media_index_service))
            .setContentText(getString(R.string.notif_content_media_index_service))
            .setSmallIcon(R.drawable.smartscan_logo)
            .setContentIntent(activityPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            getString(R.string.service_media_index_channel_id),
            getString(R.string.service_media_index_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaType = intent?.getStringExtra(EXTRA_MEDIA_TYPE) ?: TYPE_BOTH

        serviceScope.launch {
            val storage = Storage.getInstance(application)
            try {
                embeddingHandler = Embeddings(application.resources, ModelType.IMAGE)

                if(embeddingHandler == null) throw IllegalStateException("Embedding handler not initialised")

                if (mediaType == TYPE_IMAGE || mediaType == TYPE_BOTH) {
                    listener = ImageIndexListener
                    val imageIndexer = ImageIndexer(application, listener)
                    val ids = queryAllImageIds()
                    imageIndexer.indexImages(ids, embeddingHandler!!)
                }

                if (mediaType == TYPE_VIDEO || mediaType == TYPE_BOTH) {
                    listener = VideoIndexListener
                    val videoIndexer = VideoIndexer(application, listener)
                    val ids = queryAllVideoIds()
                    videoIndexer.indexVideos(ids, embeddingHandler!!)
                }
            } catch (e: CancellationException) {
                // cancelled
            } catch (t: Throwable) {
                Log.e(TAG, "Indexing failed:", t)
            } finally {
                storage.setItem("lastIndexed", System.currentTimeMillis().toString())
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        embeddingHandler?.closeSession()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun queryAllImageIds(): List<Long> {
        val imageIds = mutableListOf<Long>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                imageIds.add(cursor.getLong(idColumn))
            }
        }
        return imageIds
    }

    private fun queryAllVideoIds(): List<Long> {
        val videoIds = mutableListOf<Long>()
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        applicationContext.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            while (cursor.moveToNext()) {
                videoIds.add(cursor.getLong(idColumn))
            }
        }
        return videoIds
    }
}
