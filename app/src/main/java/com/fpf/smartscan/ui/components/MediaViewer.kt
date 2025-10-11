package com.fpf.smartscan.ui.components

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.lib.DEFAULT_IMAGE_DISPLAY_SIZE
import com.fpf.smartscan.lib.openImageInGallery
import com.fpf.smartscan.lib.openVideoInGallery
import com.fpf.smartscan.ui.screens.search.MediaType

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.fpf.smartscan.lib.canOpenUri

@Composable
fun MediaViewer(
    uri: Uri,
    type: MediaType,
    onClose: () -> Unit,
    maxSize: Int = DEFAULT_IMAGE_DISPLAY_SIZE
){
    var isActionsVisible by remember { mutableStateOf(true) }

    Popup(
        onDismissRequest = { onClose() },
        properties = PopupProperties(dismissOnBackPress = true,focusable = true)

    ) {
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (type == MediaType.IMAGE) {
                ImageDisplay(
                    uri = uri,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { isActionsVisible = !isActionsVisible }
                            )
                        },
                    contentScale = ContentScale.FillWidth,
                    type = type,
                    maxSize = maxSize
                )
            } else {
                VideoDisplay(
                    uri = uri,
                    modifier = Modifier.fillMaxSize(),
                    onTap = { isActionsVisible = !isActionsVisible }
                )
            }
            ActionRow(uri=uri, type=type, onClose, isVisible = isActionsVisible)
        }
    }
}


@Composable
fun ActionRow(
    uri: Uri,
    type: MediaType,
    onClose: () -> Unit,
    isVisible: Boolean
){
    val context = LocalContext.current
    val mime = context.contentResolver.getType(uri)
    val shareIntent: Intent = Intent().apply {
        this.action = Intent.ACTION_SEND
        this.putExtra(Intent.EXTRA_STREAM, uri)
        this.type = mime
    }
    val clipboard = LocalClipboard.current
    val isUriAccessible = canOpenUri(context, uri)

    ActionRowWithFade(visible = isVisible) {
        IconButton(
            onClick = { onClose() },
            modifier = Modifier
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close Image",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        if(isUriAccessible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            )
            {
                IconButton(onClick = {
                    clipboard.nativeClipboard.setPrimaryClip(
                        ClipData.newUri(
                            context.contentResolver,
                            "smartscan_media",
                            uri
                        )
                    )
                }) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy to clipboard",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = {
                    if (type == MediaType.IMAGE) {
                        openImageInGallery(context, uri)
                    } else {
                        openVideoInGallery(context, uri)
                    }
                }) {
                    Icon(
                        Icons.Filled.PhotoLibrary,
                        contentDescription = "Open in Gallery",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Prevent share if mime is undefined for some reason
                mime?.let {
                    IconButton(onClick = {
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ActionRowWithFade(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else -30f,
        animationSpec = tween(durationMillis = 300)
    )

    if (alpha > 0f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer() {
                    this.alpha = alpha
                    this.translationY = translationY
                }
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            content()
        }
    }
}