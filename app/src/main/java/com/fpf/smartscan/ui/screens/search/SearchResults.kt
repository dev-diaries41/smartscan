package com.fpf.smartscan.ui.screens.search

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.lib.openImageInGallery
import com.fpf.smartscan.ui.components.ImageDisplay
import com.fpf.smartscan.ui.components.ImageDisplayType
import com.fpf.smartscan.ui.components.MediaExpandedView

@Composable
fun SearchResults(
    resultToView: Uri? = null,
    mainResult: Uri,
    similarResults: List<Uri>,
    toggleViewResult: (uri: Uri?) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            TextButton(
//                onClick = {onClear() },
//            ) {
//                Text("Clear Results")
//            }
//
//            Row {
//                IconButton(onClick = { openImageInGallery(context, mainResult) }) {
//                    Icon(Icons.Filled.Image, contentDescription = "Open in Gallery", tint = MaterialTheme.colorScheme.primary)
//                }
//
//                IconButton(onClick = { mainResult = initialMainResult }) {
//                    Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Image", tint = MaterialTheme.colorScheme.primary)
//                }
//
//                IconButton(onClick = { isExpanded = true }) {
//                    Icon(Icons.Filled.Fullscreen, contentDescription = "Expand Image", tint = MaterialTheme.colorScheme.primary)
//                }
//            }
//        }


        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            ImageDisplay(
                uri = mainResult,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = { toggleViewResult(mainResult) }),
                contentScale = ContentScale.Crop,
                type = ImageDisplayType.IMAGE,
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
                            .aspectRatio(1f),
                        shape = MaterialTheme.shapes.small,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        ImageDisplay(
                            uri = uri,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = { toggleViewResult(uri) }),
                            contentScale = ContentScale.Crop,
                            type = ImageDisplayType.IMAGE
                        )
                    }
                }
            }
        }
    }

    if (resultToView != null) {
        MediaExpandedView(
            uri = resultToView,
            type = ImageDisplayType.IMAGE,
            onClose = {toggleViewResult(null)}
        )
    }
}
