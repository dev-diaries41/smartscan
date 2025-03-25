package com.fpf.smartscan.lib

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile

fun moveFile(context: Context, sourceUri: Uri, destinationDirUri: Uri): Boolean {
    val tag = "FileOperationError"
    try {
        val destDir = DocumentFile.fromTreeUri(context, destinationDirUri)
        if (destDir == null || !destDir.isDirectory) {
            Log.e(tag, "Destination is not a valid directory")
            return false
        }

        val sourceDocument = DocumentFile.fromSingleUri(context, sourceUri)
        val sourceFileName = sourceDocument?.name ?: "IMG_${System.currentTimeMillis()}.jpg"
        val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"

        val newFile = destDir.createFile(mimeType, sourceFileName)
        if (newFile == null) {
            Log.e(tag, "Failed to create new file in destination directory")
            return false
        }

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }
        sourceDocument?.delete()
        return true
    } catch (e: Exception) {
        Log.e(tag, "Failed to move image: ${e.message ?: "Unknown error"}")
        return false
    }
}

fun getDirectoryName(context: Context, uri: Uri): String {
    val documentDir = DocumentFile.fromTreeUri(context, uri)
    return documentDir?.name.toString()
}

