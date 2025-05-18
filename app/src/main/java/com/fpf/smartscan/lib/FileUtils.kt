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
        if (destDir == null || !destDir.isDirectory) {
            Log.e(tag, "Destination is not a valid directory")
            return null
        }

        val sourceDocument = DocumentFile.fromSingleUri(context, sourceUri)
        val sourceFileName = sourceDocument?.name ?: "IMG_${System.currentTimeMillis()}.jpg"
        val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"

        val newFile = destDir.createFile(mimeType, sourceFileName)
        if (newFile == null) {
            Log.e(tag, "Failed to create new file in destination directory")
            return null
        }

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }
        sourceDocument?.delete()
        return newFile.uri
    } catch (e: Exception) {
        Log.e(tag, "Failed to move image: ${e.message ?: "Unknown error"}")
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


fun deleteLocalFile(context: Context, fileName: String): Boolean {
    val file = File(context.filesDir, fileName)
    return if (file.exists()) {
        file.delete()
    } else {
        false
    }
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

