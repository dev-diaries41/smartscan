package com.fpf.smartscan.lib

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object ShareManager {

    private const val TAG = "ShareManager"

    fun shareFile(
        context: Context,
        uri: Uri,
        shareTitle: String = "Sdílet"
    ) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(uri)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, shareTitle).apply {
                // Potřebné pro Application context - umožňuje spustit aktivitu mimo Activity kontext
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při sdílení souboru", e)
        }
    }

    fun shareFiles(
        context: Context,
        uris: List<Uri>,
        shareTitle: String = "Sdílet"
    ) {
        if (uris.isEmpty()) {
            Log.w(TAG, "Prázdný seznam souborů ke sdílení")
            return
        }

        try {
            if (uris.size == 1) {
                shareFile(context, uris.first(), shareTitle)
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList(uris)
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, shareTitle).apply {
                // Potřebné pro Application context - umožňuje spustit aktivitu mimo Activity kontext
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při sdílení souborů", e)
        }
    }

    private fun getMimeType(uri: Uri): String {
        return when {
            uri.toString().contains("video", ignoreCase = true) -> "video/*"
            uri.toString().contains("image", ignoreCase = true) -> "image/*"
            else -> "image/*"
        }
    }
}
