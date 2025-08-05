package com.fpf.smartscan.ui.screens.search

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.components.ImageDisplay
import com.fpf.smartscan.ui.components.MediaExpandedView

@Composable
fun SearchResults(
    isVisible: Boolean,
    resultToView: Uri? = null,
    searchResults: List<Uri>,
    maxResults: Int,
    toggleViewResult: (uri: Uri?) -> Unit,
    type: MediaType,
) {
    if(!isVisible) return

    val mainResult = searchResults.first()
    val similarResults = if (searchResults.size > 1) searchResults.drop(1).take(maxResults) else emptyList()
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                type = type,
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
                            type = type
                        )
                    }
                }
            }
        }
    }

    if (resultToView != null) {
        MediaExpandedView(
            uri = resultToView,
            type = type,
            onClose = {toggleViewResult(null)}
        )
    }
}
