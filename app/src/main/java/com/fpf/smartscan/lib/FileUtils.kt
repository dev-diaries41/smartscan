package com.fpf.smartscan.lib

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONException
import java.io.File

fun moveFile(context: Context, sourceUri: Uri, destinationDirUri: Uri): Uri? {
    val tag = "FileOperationError"
    try {
        val destDir = DocumentFile.fromTreeUri(context, destinationDirUri)
        val sourceDocument = DocumentFile.fromSingleUri(context, sourceUri)
        if (destDir == null || !destDir.isDirectory || sourceDocument == null || !sourceDocument.exists()) {
            Log.e(tag, "Invalid source or destination")
            return null
        }

        val sourceFileName = sourceDocument.name ?: "IMG_${System.currentTimeMillis()}.jpg"
        val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"
        val newFile = destDir.createFile(mimeType, sourceFileName) ?: run {
            Log.e(tag, "Failed to create file in destination")
            return null
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
            return null
        }

        sourceDocument.delete()
        return newFile.uri
    } catch (e: Exception) {
        Log.e(tag, "Failed to move file: ${e.message ?: "Unknown error"}")
        return null
    }
}


fun getDirectoryName(context: Context, uri: Uri): String {
    val documentDir = DocumentFile.fromTreeUri(context, uri)
    return documentDir?.name.toString()
}

fun getFilesFromDir(context: Context, uris: List<Uri>, fileExtensions: List<String>): List<Uri> {
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

    return fileUris
}

fun readUriListFromFile(path: String): List<Uri> {
    val file = File(path)
    if (!file.exists()) {
        Log.e("UriReader", "File not found: $path")
        return emptyList()
    }

    val content = file.readText()
    val jsonArray = try {
        JSONArray(content)
    } catch (e: JSONException) {
        Log.e("UriReader", "Invalid JSON in file: $path", e)
        return emptyList()
    }

    val uriList = mutableListOf<Uri>()
    for (i in 0 until jsonArray.length()) {
        jsonArray.optString(i, null)?.let {
            uriList.add(it.toUri())
        }
    }

    return uriList
}

fun canOpenUri(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { }
        true
    } catch (e: Exception) {
        false
    }
}

