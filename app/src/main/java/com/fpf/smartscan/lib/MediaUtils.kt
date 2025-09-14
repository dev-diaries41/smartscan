package com.fpf.smartscan.lib

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import androidx.core.graphics.scale
import androidx.documentfile.provider.DocumentFile
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig
import com.fpf.smartscansdk.core.utils.getBitmapFromUri
import com.fpf.smartscansdk.core.utils.getScaledDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

const val DEFAULT_IMAGE_DISPLAY_SIZE = 1024

/**
 * A simple LRU Cache to hold Bitmaps to avoid decoding them multiple times.
 */
object BitmapCache {
    private val cache: LruCache<Uri, Bitmap> = object : LruCache<Uri, Bitmap>(calculateMemoryCacheSize()) {
        override fun sizeOf(key: Uri, value: Bitmap): Int {
            return value.byteCount / 1024 // in KB
        }
    }

    private fun calculateMemoryCacheSize(): Int {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt() // in KB
        val calculatedCacheSize = maxMemory / 8
        val maxAllowedCacheSize = 50 * 1024

        return if (calculatedCacheSize > maxAllowedCacheSize) {
            maxAllowedCacheSize
        } else {
            calculatedCacheSize
        }
    }

    fun get(uri: Uri): Bitmap? = cache.get(uri)
    fun put(uri: Uri, bitmap: Bitmap): Bitmap? = cache.put(uri, bitmap)
}



suspend fun loadBitmapFromUri(context: Context, uri: Uri, maxSize: Int = DEFAULT_IMAGE_DISPLAY_SIZE): Bitmap? = withContext(Dispatchers.IO) {
    BitmapCache.get(uri) ?: try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val (w, h) = getScaledDimensions(imgWith  = info.size.width, imgHeight = info.size.height, maxSize)
            decoder.setTargetSize(w, h)
        }
        BitmapCache.put(uri, bitmap)
        bitmap
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun getImageUriFromId(id: Long): Uri {
    return ContentUris.withAppendedId(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        id
    )
}

suspend fun fetchBitmapsFromDirectory(context: Context, directoryUri: Uri, maxSize: Int = ClipConfig.IMAGE_SIZE_X, limit: Int? = null): List<Bitmap> = withContext(Dispatchers.IO) {
    val directory = DocumentFile.fromTreeUri(context, directoryUri)
        ?: throw IllegalArgumentException("Invalid directory URI: $directoryUri")

    val validExtensions = listOf("png", "jpg", "jpeg", "bmp", "gif")
    val imageFiles = directory.listFiles()
        .filter { doc ->
            doc.isFile && (doc.name?.substringAfterLast('.', "")?.lowercase(Locale.getDefault()) in validExtensions)
        }
        .shuffled()
        .let { if (limit != null) it.take(limit) else it }

    if (imageFiles.isEmpty()) {
        throw IllegalStateException("No valid image files found in directory: $directoryUri")
    }

    imageFiles.mapNotNull { doc ->
        try {
            getBitmapFromUri(context, doc.uri, maxSize)
        } catch (e: Exception) {
            Log.e("BitmapFetch", "Failed to load image: ${doc.uri}", e)
            null
        }
    }
}

fun openImageInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}


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
