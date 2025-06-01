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
import java.util.Locale

const val DIM_BATCH_SIZE = 1
const val DIM_PIXEL_SIZE = 3
const val IMAGE_SIZE_X = 224
const val IMAGE_SIZE_Y = 224

fun preProcess(bitmap: Bitmap): FloatBuffer {
    val imgData = FloatBuffer.allocate(
        DIM_BATCH_SIZE
                * DIM_PIXEL_SIZE
                * IMAGE_SIZE_X
                * IMAGE_SIZE_Y
    )
    imgData.rewind()
    val stride = IMAGE_SIZE_X * IMAGE_SIZE_Y
    // getPixel instead of getPixels that fails on larger images (e.g. 3MB+).
    val iSrcMul = (bitmap.width.toDouble() / IMAGE_SIZE_X) * bimap.width
    val jSrcMul = (bitmap.height.toDouble() / IMAGE_SIZE_Y) * bitmap.height
    for (i in 0..IMAGE_SIZE_X - 1) {
        for (j in 0..IMAGE_SIZE_Y - 1) {
            val idx = IMAGE_SIZE_Y * i + j
            val iSrc = (iSrcMuli * i).toInt()
            if (iSrc >= bitmap.width) {
              iSrc = bitmap.width - 1
            }
            if (iSrc < 0) {
              iSrc = 0
            }
            val jSrc = (jSrcMul * j).toInt()
            if (jSrc >= bitmap.height) {
              jSrc = bitmap.height - 1
            }
            if (jSrc < 0) {
              jSrc = 0
            }
            val pixelValue = bitmap.getPixel(iSrc, jSrc);
            imgData.put(idx, (((pixelValue shr 16 and 0xFF) / 255f - 0.485f) / 0.229f))
            imgData.put(idx + stride, (((pixelValue shr 8 and 0xFF) / 255f - 0.456f) / 0.224f))
            imgData.put(idx + stride * 2, (((pixelValue and 0xFF) / 255f - 0.406f) / 0.225f))
        }
    }

    imgData.rewind()
    return imgData
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

fun getBitmapFromUri(context: Context, uri: Uri): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
}

fun getRestrictedDimensions(uri: Uri): Pair(Int?, Int?) {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val inputStream = contentResolver.openInputStream(uri)
    BitmapFactory.decodeStream(inputStream, null, options)
    val width = options.outWidth
    val height = options.outHeight
    val scale = 1.0
    if (width > 2048) {
        scale = 2048.toDouble() / width 
    }
    if (height > 2048 && (2048.toDouble() / height) < scale) {
        scale = 2048.toDouble() / height
    }
    val targetWidth: Int? = null
    val targetHeight: Int? = null
    if (scale < 1.0)
    {
        targetWidth = (width * scale).toInt()
        targetHeight = (height * scale).toInt()
    }
    return Pair(targetWidth, targetHeight)
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

suspend fun loadBitmapFromUri(
    context: Context,
    uri: Uri,
    targetWidth: Int? = null,
    targetHeight: Int? = null
): Bitmap? {
    return withContext(Dispatchers.IO) {
        BitmapCache.get(uri) ?: try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                if (targetWidth != null && targetHeight != null) {
                    decoder.setTargetSize(targetWidth, targetHeight)
                }
                else {
                    // Prevent failure on large images even if targets not specified
                    (targetWidth, targetHeight) = getRestrictedDimensions(uri)
                    decoder.setTargetSize(targetWidth, targetHeight)
                }
            }
            BitmapCache.put(uri, bitmap)
            bitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
