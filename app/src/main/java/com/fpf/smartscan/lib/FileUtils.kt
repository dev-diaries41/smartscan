package com.fpf.smartscan.lib

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.File

suspend fun moveFile(context: Context, sourceUri: Uri, destinationDirUri: Uri): Uri? = withContext(
    Dispatchers.IO) {
    val tag = "FileOperationError"
    try {
        val destDir = DocumentFile.fromTreeUri(context, destinationDirUri)
        val sourceDocument = DocumentFile.fromSingleUri(context, sourceUri)
        if (destDir == null || !destDir.isDirectory || sourceDocument == null || !sourceDocument.exists()) {
            Log.e(tag, "Invalid source or destination")
            return@withContext null
        }

        val sourceFileName = sourceDocument.name ?: "IMG_${System.currentTimeMillis()}.jpg"
        val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"
        val newFile = destDir.createFile(mimeType, sourceFileName) ?: run {
            Log.e(tag, "Failed to create file in destination")
            return@withContext null
        }

         context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }

        val originalSize = sourceDocument.length()
        val newSize = newFile.length()

        if (originalSize != newSize) {
            newFile.delete()
            Log.e(tag, "Failed to copy data")
            return@withContext null
        }

        sourceDocument.delete()
        return@withContext newFile.uri
    } catch (e: Exception) {
        Log.e(tag, "Failed to move file: ${e.message ?: "Unknown error"}")
        return@withContext null
    }
}


fun getDirectoryName(context: Context, uri: Uri): String {
    val documentDir = DocumentFile.fromTreeUri(context, uri)
    return documentDir?.name.toString()
}

suspend fun getFilesFromDir(context: Context, uris: List<Uri>, fileExtensions: List<String>): List<Uri> = withContext(
    Dispatchers.IO) {
    val fileUris = mutableListOf<Uri>()

    for (uri in uris) {
        val documentDir = DocumentFile.fromTreeUri(context, uri)
        if (documentDir != null && documentDir.isDirectory) {
            documentDir.listFiles().forEach { documentFile ->
                if (documentFile.isFile) {
                    val fileName = documentFile.name ?: ""
                    if (fileExtensions.any { fileName.endsWith(".$it", ignoreCase = true) }) {
                        fileUris.add(documentFile.uri)
                    }
                }
            }
        } else {
            Log.e("getFilesFromDir", "Invalid directory URI: $uri")
        }
    }

    return@withContext fileUris
}

suspend fun readUriListFromFile(path: String): List<Uri> = withContext(Dispatchers.IO) {
    val file = File(path)
    if (!file.exists()) {
        Log.e("UriReader", "File not found: $path")
        return@withContext emptyList()
    }

    val content = file.readText()
    val jsonArray = try {
        JSONArray(content)
    } catch (e: JSONException) {
        Log.e("UriReader", "Invalid JSON in file: $path", e)
        return@withContext emptyList()
    }

    val uriList = mutableListOf<Uri>()
    for (i in 0 until jsonArray.length()) {
        jsonArray.optString(i, null)?.let {
            uriList.add(it.toUri())
        }
    }

    return@withContext uriList
}

fun canOpenUri(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { }
        true
    } catch (e: Exception) {
        false
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val name = if (cursor != null && nameIndex != null && nameIndex != -1 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else null
        cursor?.close()
        return name
    }catch (e: Exception){
        Log.e("getFileName", "${e.message}")
        return null
    }
}

