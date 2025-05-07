package com.fpf.smartscan.ui.screens.search

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fpf.smartscan.lib.loadVideoThumbnailFromUri

@Composable
fun VideoSearchResults(
    initialMainResult: Uri,
    similarResults: List<Uri>,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var mainResult by remember { mutableStateOf(initialMainResult) }
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row with Clear and action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onClear) {
                Text("Clear Results")
            }
            Row {
                IconButton(onClick = { openVideoInGallery(context, mainResult) }) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Open Video",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { mainResult = initialMainResult }) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = "Reset Video",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { isExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Expand Video",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Main video thumbnail card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            MediaStoreVideoThumbnail(
                uri = mainResult,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Similar Videos (${similarResults.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(similarResults) { uri ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable { mainResult = uri },
                        shape = MaterialTheme.shapes.small,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        MediaStoreVideoThumbnail(
                            uri = uri,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

    // Fullscreen dialog
    if (isExpanded) {
        Dialog(onDismissRequest = { isExpanded = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                MediaStoreVideoThumbnail(
                    uri = mainResult,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { isExpanded = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MediaStoreVideoThumbnail(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    targetWidth: Int? = null,
    targetHeight: Int? = null
) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = loadVideoThumbnailFromUri(context, uri, targetWidth, targetHeight)
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Video Thumbnail",
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color.Gray))
    }
}