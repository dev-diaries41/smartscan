package com.fpf.smartscan.ui.components.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.ui.components.media.ImageUploader
import com.fpf.smartscan.ui.components.SelectorItem


@Composable
fun ImageSearcher(
    uri: Uri?,
    maxResults: Int,
    threshold: Float,
    mediaType: MediaType,
    searchEnabled: Boolean,
    mediaTypeSelectorEnabled: Boolean,
    onImageSelected: (Uri?) -> Unit,
    onMediaTypeChange: (type: MediaType) -> Unit,
    onSearch: (n: Int, threshold: Float) -> Unit,
){
    Row(
        modifier = Modifier.fillMaxWidth().background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ).padding(12.dp),

        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ImageUploader(
            uri = uri,
            onImageSelected = onImageSelected,
            placeholderText = {
                Text(
                    text = "Upload image",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.alpha(0.8f).padding(8.dp),
                )
            }
        )

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(horizontal = 12.dp))
        {
            SelectorItem(
                enabled = mediaTypeSelectorEnabled, // prevent switching modes when indexing in progress
                label = "Media type",
                showLabel = false,
                options = mediaTypeOptions.values.toList(),
                selectedOption = mediaTypeOptions[mediaType]!!,
                onOptionSelected = { selected ->
                    val newMode = mediaTypeOptions.entries
                        .find { it.value == selected }
                        ?.key ?: MediaType.IMAGE
                    onMediaTypeChange(newMode)
                }
            )
            Button(
                modifier = Modifier.width(140.dp),
                enabled = searchEnabled ,
                onClick = {onSearch(maxResults, threshold) }
            ) {
                Icon(Icons.Default.ImageSearch, contentDescription = "Image search icon", modifier = Modifier.padding(end = 4.dp))
                Text(text = "Search")
            }
        }
    }
}