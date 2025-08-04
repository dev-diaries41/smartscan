package com.fpf.smartscan.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fpf.smartscan.lib.DEFAULT_IMAGE_DISPLAY_SIZE
import com.fpf.smartscan.lib.openImageInGallery
import com.fpf.smartscan.lib.openVideoInGallery
import com.fpf.smartscan.ui.screens.search.MediaType

@Composable
fun MediaExpandedView(
    uri: Uri,
    type: MediaType,
    onClose: () -> Unit,
    maxSize: Int = DEFAULT_IMAGE_DISPLAY_SIZE
){
    val context = LocalContext.current

    Dialog(onDismissRequest = { onClose }) {
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ImageDisplay(
                uri = uri,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                type = type,
                maxSize = maxSize
            )
            IconButton(
                onClick = { onClose() },
                modifier = Modifier
                    .align(Alignment.Companion.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close Image",
                    tint = Color.White
                )
            }

            Row (
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            )
            {
                IconButton(onClick = {
                    if (type == MediaType.IMAGE) {
                        openImageInGallery(context, uri)
                    }else{
                        openVideoInGallery(context, uri)
                    }
                }) {
                    Icon(if (type == MediaType.IMAGE) Icons.Filled.Image else Icons.Filled.PlayArrow, contentDescription = "Open in Gallery", tint = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(onClick = {  }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}