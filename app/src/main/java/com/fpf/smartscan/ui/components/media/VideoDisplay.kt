package com.fpf.smartscan.ui.components.media

import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoDisplay(
    uri: Uri,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    val gestureDetector = remember(context) {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onTap() 
                return true // signal handled so we can call performClick()
            }
        })
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                setOnTouchListener { view, event ->
                    val handled = gestureDetector.onTouchEvent(event)
                    if (handled) {
                        view.performClick() // accessibility compliance
                    }
                    false // pass event to PlayerView so controls still work
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}
