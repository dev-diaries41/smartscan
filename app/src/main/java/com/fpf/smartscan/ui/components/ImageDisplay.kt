package com.fpf.smartscan.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.fpf.smartscan.lib.DEFAULT_IMAGE_DISPLAY_SIZE
import com.fpf.smartscan.lib.loadBitmapFromUri
import com.fpf.smartscan.lib.loadVideoThumbnailFromUri
import com.fpf.smartscan.ui.screens.search.MediaType


@Composable
fun ImageDisplay(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxSize: Int = DEFAULT_IMAGE_DISPLAY_SIZE,
    type: MediaType
) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = when(type) {
            MediaType.IMAGE -> loadBitmapFromUri(context, uri, maxSize)
            MediaType.VIDEO -> loadVideoThumbnailFromUri(context, uri, maxSize)
        }
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Displayed image",
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color.Gray))
    }
}
