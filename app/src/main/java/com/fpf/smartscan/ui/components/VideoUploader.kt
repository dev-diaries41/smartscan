package com.fpf.smartscan.ui.components

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoUploader(
    videoUri: Uri?,
    onVideoSelected: (Uri?) -> Unit,
) {

// Extract a thumbnail from the video using MediaMetadataRetriever
    val context = LocalContext.current
    val videoThumbnail by produceState<ImageBitmap?>(initialValue = null, key1 = videoUri) {
        value = null
        videoUri?.let { uri ->
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val durationUs =
                        (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLong()?.times(1000)?.div(2))?: 1_000_000
                    val bitmap = retriever.getFrameAtTime(durationUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        value = bitmap.asImageBitmap()
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Invalid video format or corrupted file", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error processing video", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    retriever.release()
                }
            }
        }
    }


    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            onVideoSelected(uri)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (videoUri != null) 8.dp else 4.dp)
            ) {
                if (videoThumbnail != null) {
                    Image(
                        bitmap = videoThumbnail!!,
                        contentDescription = "Uploaded Video Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No video selected",
                            modifier = Modifier.padding(bottom = 16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (videoUri == null) {
                            Button(
                                onClick = { videoPickerLauncher.launch("video/*") }
                            ) {
                                Text(text = "Upload Video")
                            }
                        }
                    }
                }
            }

            if (videoUri != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                ) {
                    IconButton(
                        onClick = { onVideoSelected(null) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSecondaryContainer)
                            .size(24.dp)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove Video",
                            tint = MaterialTheme.colorScheme.inversePrimary,
                        )
                    }
                }
            }
        }
    }
}
