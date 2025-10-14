package com.fpf.smartscan.ui.components.media

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
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import coil3.video.VideoFrameDecoder
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.lib.DEFAULT_IMAGE_DISPLAY_SIZE

@Composable
fun ImageDisplay(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxSize: Int = DEFAULT_IMAGE_DISPLAY_SIZE,
    type: MediaType
) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = uri, key2 = type) {
        val loader = ImageLoader.Builder(context).components {
                if (type == MediaType.VIDEO) {
                    add(VideoFrameDecoder.Factory())
                }
            }
            .build()

        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(maxSize, maxSize)
            .allowHardware(true)
            .target { bitmap ->
                value = bitmap.toBitmap()
            }
            .build()

        loader.enqueue(request)
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
        Box(modifier = modifier.background(Color.Transparent))
    }
}
