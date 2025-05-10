package com.fpf.smartscan.lib

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun extractFramesFromVideo(context: Context, videoUri: Uri, frameCount: Int = 10): List<Bitmap>? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, videoUri)

        val durationUs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.times(1000)
            ?: return null

        val frameList = mutableListOf<Bitmap>()

        for (i in 0 until frameCount) {
            val frameTimeUs = (i * durationUs) / frameCount
            val bitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            if (bitmap != null) {
                frameList.add(bitmap)
            } else {
                // Temporary Fix: Break early if null which suggest codec issue with video
                break
            }
        }

        if (frameList.isEmpty()) return null

        frameList
    } catch (e: Exception) {
        Log.e("FrameExtractionError", "Error extracting video frames: $e")
        null
    } finally {
        retriever.release()
    }
}


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