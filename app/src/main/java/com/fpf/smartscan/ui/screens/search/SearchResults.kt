package com.fpf.smartscan.ui.screens.search

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.LruCache
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fpf.smartscan.lib.loadBitmapFromUri

@Composable
fun MediaStoreImage(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    targetWidth: Int? = null,
    targetHeight: Int? = null
) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = loadBitmapFromUri(context, uri, targetWidth, targetHeight)
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color.Gray))
    }
}

fun openImageInGallery(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}

@Composable
fun SearchResults(
    initialMainResult: Uri,
    similarResults: List<Uri>,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var mainResult by remember { mutableStateOf(initialMainResult) }
    var isExpanded by remember { mutableStateOf(false) }  // State to track full-screen mode

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {onClear() },
            ) {
                Text("Clear Results")
            }

            Row {
                IconButton(onClick = { openImageInGallery(context, mainResult) }) {
                    Icon(Icons.Filled.Image, contentDescription = "Open in Gallery", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = { mainResult = initialMainResult }) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Image", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = { isExpanded = true }) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "Expand Image", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }


        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            MediaStoreImage(
                uri = mainResult,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Similar Results (${similarResults.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)  //  fixed height required to bounds the grid's maximum height.
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
                        MediaStoreImage(
                            uri = uri,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

        if (isExpanded) {
        Dialog(onDismissRequest = { isExpanded = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                MediaStoreImage(
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
                        contentDescription = "Minimize Image",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
