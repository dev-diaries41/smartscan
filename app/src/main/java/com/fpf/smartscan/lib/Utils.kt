package com.fpf.smartscan.lib

import android.Manifest
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fpf.smartscan.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.any

fun toDateString(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getTimeInMinutesAndSeconds(milliseconds: Long): Pair<Long, Long> {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    return Pair(minutes, seconds % 60)
}

fun showNotification(context: Context, title: String, text: String, id: Int = 1001) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }
    val channelId = context.getString(R.string.worker_channel_id)
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.smartscan_logo)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        notify(id, notificationBuilder.build())
    }
}

fun isServiceRunning(context: Context, serviceClass: Class<out Service>): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return am.getRunningServices(Int.MAX_VALUE).any {
        it.service.className == serviceClass.name
    }
}

suspend fun isWorkScheduled(context: Context, workName: String): Boolean {
    return withContext(Dispatchers.IO) {
        val workManager = WorkManager.getInstance(context)
        val workInfoList = workManager.getWorkInfosForUniqueWork(workName).get()
        workInfoList.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    }
}

fun cancelWorker(context: Context, uniqueWorkName: String?, tag: String?){
    val workManager = WorkManager.getInstance(context.applicationContext)
    uniqueWorkName?.let{workManager.cancelUniqueWork(it)}
    tag?.let{workManager.cancelAllWorkByTag(it)}
}
