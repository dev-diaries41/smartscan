package com.fpf.smartscan.lib

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import java.nio.FloatBuffer
import androidx.core.graphics.scale
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

const val DIM_BATCH_SIZE = 1
const val DIM_PIXEL_SIZE = 3
const val IMAGE_SIZE_X = 224
const val IMAGE_SIZE_Y = 224
const val DEFAULT_IMAGE_DISPLAY_SIZE = 1024

fun preProcess(bitmap: Bitmap): FloatBuffer {
    val cropped = centerCrop(bitmap, IMAGE_SIZE_X)

    val numFloats = DIM_BATCH_SIZE * DIM_PIXEL_SIZE * IMAGE_SIZE_Y * IMAGE_SIZE_X
    val byteBuffer = ByteBuffer
        .allocateDirect(numFloats * 4)
        .order(ByteOrder.nativeOrder())
    val floatBuffer = byteBuffer.asFloatBuffer()
    for (c in 0 until DIM_PIXEL_SIZE) {
        for (y in 0 until IMAGE_SIZE_Y) {
            for (x in 0 until IMAGE_SIZE_X) {
                val px = cropped.getPixel(x, y)
                val v = when (c) {
                    0 -> (px shr 16 and 0xFF) / 255f  // R
                    1 -> (px shr  8 and 0xFF) / 255f  // G
                    else -> (px and 0xFF) / 255f  // B
                }
                val norm = when (c) {
                    0 -> (v - 0.485f) / 0.229f
                    1 -> (v - 0.456f) / 0.224f
                    else -> (v - 0.406f) / 0.225f
                }
                floatBuffer.put(norm)
            }
        }
    }
    floatBuffer.rewind()
    return floatBuffer
}

fun centerCrop(bitmap: Bitmap, imageSize: Int): Bitmap {
    val cropX: Int
    val cropY: Int
    val cropSize: Int
    if (bitmap.width >= bitmap.height) {
        cropX = bitmap.width / 2 - bitmap.height / 2
        cropY = 0
        cropSize = bitmap.height
    } else {
        cropX = 0
        cropY = bitmap.height / 2 - bitmap.width / 2
        cropSize = bitmap.width
    }
    var bitmapCropped = Bitmap.createBitmap(
        bitmap, cropX, cropY, cropSize, cropSize
    )
    bitmapCropped = bitmapCropped.scale(imageSize, imageSize, false)
    return bitmapCropped
}

fun getScaledDimensions(imgWith: Int, imgHeight: Int, maxSize: Int = 1024): Pair<Int, Int> {
    if (imgWith <= maxSize && imgHeight <= maxSize) {
        return imgWith to imgHeight
    }
    return if (imgWith >= imgHeight) {
        val scale = maxSize.toFloat() / imgWith
        maxSize to (imgHeight * scale).toInt()
    } else {
        val scale = maxSize.toFloat() / imgHeight
        (imgWith * scale).toInt() to maxSize
    }
}

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

fun getBitmapFromUri(context: Context, uri: Uri, maxSize: Int = IMAGE_SIZE_X): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val (w, h) = getScaledDimensions(info.size.width, info.size.height, maxSize)
        decoder.setTargetSize(w, h)
    }.copy(Bitmap.Config.ARGB_8888, true)
}


fun getImageUriFromId(id: Long): Uri {
    return ContentUris.withAppendedId(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        id
    )
}

suspend fun fetchBitmapsFromDirectory(context: Context, directoryUri: Uri, limit: Int? = null): List<Bitmap> = withContext(Dispatchers.IO) {
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
            getBitmapFromUri(context, doc.uri)
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

