package com.fpf.smartscan.lib

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig
import com.fpf.smartscansdk.core.utils.getBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

const val DEFAULT_IMAGE_DISPLAY_SIZE = 1024

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

fun openVideoInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}


fun queryImageIds(context: Context, dirUris: List<Uri>): List<Long> {
    val imageIds = mutableListOf<Long>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()
    val envRoot = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')

    if (dirUris.isNotEmpty()) {
        for (uri in dirUris) {
            try {
                // Handle tree/document URIs like "primary:Pictures/MyFolder"
                if (DocumentsContract.isTreeUri(uri) || uri.authority == "com.android.externalstorage.documents") {
                    val docId = DocumentsContract.getTreeDocumentId(uri)
                    val afterColon = docId.substringAfter(':', "")
                    if (afterColon.isNotEmpty()) {
                        val rel = afterColon.trim('/') + "/" // RELATIVE_PATH usually ends with '/'
                        selectionParts.add("${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
                        selectionArgs.add("$rel%")
                        continue
                    }
                }

                // Handle file:// uris
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: continue)
                    val absPath = file.absolutePath.trimEnd('/') + "/"
                    // RELATIVE_PATH if possible (strip envRoot)
                    if (absPath.startsWith(envRoot)) {
                        val rel = absPath.removePrefix(envRoot).trimStart('/').trimEnd('/') + "/"
                        selectionParts.add("${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
                        selectionArgs.add("$rel%")
                    }
                    continue
                }

                // Fallback attempt: use last path segment as folder name
                val seg = uri.path?.trim('/') ?: uri.lastPathSegment ?: continue
                if (seg.isNotEmpty()) {
                    val folderLike = if (seg.endsWith("/")) seg else "$seg/"
                    selectionParts.add("${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
                    selectionArgs.add("%$folderLike%")
                }
            } catch (_: Exception) {
                // ignore malformed uri and continue
            }
        }
    }

    val selection: String?
    val args: Array<String>?
    if (selectionParts.isEmpty()) {
        selection = null
        args = null
    } else {
        selection = selectionParts.joinToString(" OR ", prefix = "(", postfix = ")")
        args = selectionArgs.toTypedArray()
    }

    context.applicationContext.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        args,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            imageIds.add(cursor.getLong(idColumn))
        }
    }
    return imageIds
}

fun queryVideoIds(context: Context, dirUris: List<Uri>): List<Long> {
    val videoIds = mutableListOf<Long>()
    val projection = arrayOf(MediaStore.Video.Media._ID)
    val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()
    val envRoot = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')

    if (dirUris.isNotEmpty()) {
        for (uri in dirUris) {
            try {
                // Handle tree/document URIs like "primary:Movies/MyFolder"
                if (DocumentsContract.isTreeUri(uri) || uri.authority == "com.android.externalstorage.documents") {
                    val docId = DocumentsContract.getTreeDocumentId(uri)
                    val afterColon = docId.substringAfter(':', "")
                    if (afterColon.isNotEmpty()) {
                        val rel = afterColon.trim('/') + "/"
                        selectionParts.add("${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?")
                        selectionArgs.add("$rel%")
                        continue
                    }
                }

                // Handle file:// URIs
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: continue)
                    val absPath = file.absolutePath.trimEnd('/') + "/"
                    if (absPath.startsWith(envRoot)) {
                        val rel = absPath.removePrefix(envRoot).trimStart('/').trimEnd('/') + "/"
                        selectionParts.add("${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?")
                        selectionArgs.add("$rel%")
                    }
                    continue
                }

                // Fallback: use last path segment
                val seg = uri.path?.trim('/') ?: uri.lastPathSegment ?: continue
                if (seg.isNotEmpty()) {
                    val folderLike = if (seg.endsWith("/")) seg else "$seg/"
                    selectionParts.add("${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?")
                    selectionArgs.add("%$folderLike%")
                }
            } catch (_: Exception) {
                // ignore malformed uri and continue
            }
        }
    }

    val selection: String?
    val args: Array<String>?
    if (selectionParts.isEmpty()) {
        selection = null
        args = null
    } else {
        selection = selectionParts.joinToString(" OR ", prefix = "(", postfix = ")")
        args = selectionArgs.toTypedArray()
    }

    context.applicationContext.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        args,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        while (cursor.moveToNext()) {
            videoIds.add(cursor.getLong(idColumn))
        }
    }
    return videoIds
}
