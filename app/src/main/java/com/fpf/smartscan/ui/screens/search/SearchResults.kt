package com.fpf.smartscan.ui.screens.search

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.components.ImageDisplay

@Composable
fun SearchResults(
    isVisible: Boolean,
    searchResults: List<Uri>,
    toggleViewResult: (uri: Uri?) -> Unit,
    type: MediaType,
    numGridColumns: Int = 3,
) {
    if (!isVisible) return

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Results (${searchResults.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(numGridColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(searchResults) { uri ->
                Card(
                    modifier = Modifier.aspectRatio(1f).padding(2.dp),
                    shape = RectangleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    ImageDisplay(
                        uri = uri,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(0.5.dp)
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
}