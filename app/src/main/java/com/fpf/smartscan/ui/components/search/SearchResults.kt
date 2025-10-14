package com.fpf.smartscan.ui.components.search

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.ui.components.media.ImageDisplay

@Composable
fun SearchResults(
    isVisible: Boolean,
    searchResults: List<Uri>,
    toggleViewResult: (uri: Uri?) -> Unit,
    type: MediaType,
    onLoadMore: () -> Unit,
    totalResults: Int,
    numGridColumns: Int = 3,
    loadMoreBuffer: Int = 5
) {
    if (!isVisible) return

    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow false
            val lastVisibleItem = visibleItems.last().index
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem + loadMoreBuffer >= totalItems && totalItems < totalResults
        }.collect { shouldLoadMore ->
                if (shouldLoadMore) onLoadMore()
            }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "${searchResults.size} Results",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(4.dp)
        )
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(numGridColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(searchResults) { uri ->
                ImageDisplay(
                    uri = uri,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { toggleViewResult(uri) },
                    contentScale = ContentScale.Crop,
                    type = type
                )
            }
        }
    }
}