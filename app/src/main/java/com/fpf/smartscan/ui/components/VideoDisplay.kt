package com.fpf.smartscan.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.fpf.smartscan.lib.DEFAULT_IMAGE_DISPLAY_SIZE


@Composable
fun VideoDisplay(
    uri: Uri,
    modifier: Modifier = Modifier,
    maxSize: Int = DEFAULT_IMAGE_DISPLAY_SIZE
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                layoutParams =
                    android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                    )
            }
        },
        modifier = modifier
            .sizeIn(maxWidth = maxSize.dp, maxHeight = maxSize.dp)
            .background(Color.Black)
    )
}
