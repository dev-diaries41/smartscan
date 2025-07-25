package com.fpf.smartscan.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fpf.smartscan.lib.DEFAULT_IMAGE_DISPLAY_SIZE

@Composable
fun MediaExpandedView(
    uri: Uri,
    type: ImageDisplayType,
    onClose: () -> Unit,
    maxSize: Int = DEFAULT_IMAGE_DISPLAY_SIZE
){
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
                onClick = { onClose },
                modifier = Modifier
                    .align(Alignment.Companion.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close Image",
                    tint = Color.White
                )
            }
        }
    }
}