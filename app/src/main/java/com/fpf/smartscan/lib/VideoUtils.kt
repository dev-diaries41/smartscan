package com.fpf.smartscan.lib

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fpf.smartscansdk.core.utils.getScaledDimensions


fun getVideoUriFromId(id: Long): Uri {
    return ContentUris.withAppendedId(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        id
    )
}

suspend fun loadVideoThumbnailFromUri(
    context: Context,
    uri: Uri,
    maxSize: Int
): Bitmap? {
    return withContext(Dispatchers.IO) {
        BitmapCache.get(uri) ?: try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            var bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            if (bitmap != null) {
                val (w, h) = getScaledDimensions(imgWith  = bitmap.width, imgHeight = bitmap.height, maxSize)
                bitmap = bitmap.scale(w, h)
            }
            bitmap?.let { BitmapCache.put(uri, it) }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun openVideoInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}