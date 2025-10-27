package com.fpf.smartscan.lib

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Manager pro sdílení obrázků a videí pomocí Android Share Sheet.
 * Podporuje sdílení jednoho nebo více souborů.
 *
 * Pro MediaStore content:// URI není potřeba FileProvider.
 */
object ShareManager {

    private const val TAG = "ShareManager"

    /**
     * Sdílí jeden soubor (obrázek nebo video).
     *
     * @param context Android context
     * @param uri Content URI souboru (z MediaStore)
     * @param shareTitle Titulek share sheetu
     */
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

            val chooser = Intent.createChooser(shareIntent, shareTitle)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při sdílení souboru: ${uri}", e)
        }
    }

    /**
     * Sdílí více souborů (obrázky a/nebo videa).
     *
     * @param context Android context
     * @param uris List content URIs souborů
     * @param shareTitle Titulek share sheetu
     */
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
            // ACTION_SEND pro jeden soubor, ACTION_SEND_MULTIPLE pro více
            if (uris.size == 1) {
                shareFile(context, uris.first(), shareTitle)
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*" // Většina souborů jsou obrázky
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList(uris)
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, shareTitle)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při sdílení souborů (${uris.size})", e)
        }
    }

    /**
     * Detekuje MIME type podle URI.
     * Fallback na "image/*" pokud nelze určit.
     */
    private fun getMimeType(uri: Uri): String {
        return when {
            uri.toString().contains("video", ignoreCase = true) -> "video/*"
            uri.toString().contains("image", ignoreCase = true) -> "image/*"
            else -> "image/*" // Default fallback
        }
    }
}
