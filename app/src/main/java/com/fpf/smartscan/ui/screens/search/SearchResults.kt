package com.fpf.smartscan.ui.screens.search

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.components.ImageDisplay

@Composable
fun SearchResults(
    isVisible: Boolean,
    searchResults: List<Uri>,
    maxResults: Int,
    toggleViewResult: (uri: Uri?) -> Unit,
    type: MediaType,
    numGridColumns: Int = 3,
) {
    if (!isVisible) return

    val mainResult = searchResults.first()
    val similarResults = if (searchResults.size > 1) searchResults.drop(1).take(maxResults) else emptyList()

    Column() {
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
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        toggleViewResult(mainResult)
                    },
                contentScale = ContentScale.Crop,
                type = type
            )
        }


        Text(
            text = "Similar Results (${similarResults.size})",
            textAlign = TextAlign.Left,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        Column() {
            similarResults.chunked(numGridColumns).forEachIndexed { index, uris ->
                val scale = (uris.size / numGridColumns.toFloat())
                Row(
                    modifier = Modifier.fillMaxSize(scale)
                ) {
                    uris.forEachIndexed { batchIdx, uri ->
                        Card(
                            modifier =  Modifier
                                .weight((1 / numGridColumns.toFloat()))
                                .aspectRatio(1f)
                                .padding(4.dp),
                            shape = MaterialTheme.shapes.small,
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            ImageDisplay(
                                uri = uri,
                                modifier = Modifier
                                    .fillMaxSize()
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
    }
}