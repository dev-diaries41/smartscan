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

fun getVideoUriFromId(id: Long): Uri {
    return ContentUris.withAppendedId(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        id
    )
}

/**
 * Load a video thumbnail bitmap using MediaMetadataRetriever.
 * Optionally scales the bitmap if targetWidth and targetHeight are provided.
 */
suspend fun loadVideoThumbnailFromUri(
    context: Context,
    uri: Uri,
    targetWidth: Int? = null,
    targetHeight: Int? = null
): Bitmap? {
    return withContext(Dispatchers.IO) {
        // Check cache first
        BitmapCache.get(uri) ?: try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            // Get a frame at time 0 (or adjust as needed)
            var bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            // Optionally scale the bitmap if dimensions are provided
            if (bitmap != null && targetWidth != null && targetHeight != null) {
                bitmap = bitmap.scale(targetWidth, targetHeight)
            }
            // Cache the bitmap
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