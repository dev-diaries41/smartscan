package com.fpf.smartscan.ui.components.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.ui.components.SelectorItem
import com.fpf.smartscan.ui.components.media.ImageDisplay


@Composable
fun ImageSearcher(
    uri: Uri?,
    threshold: Float,
    mediaType: MediaType,
    searchEnabled: Boolean,
    mediaTypeSelectorEnabled: Boolean,
    onMediaTypeChange: (type: MediaType) -> Unit,
    onSearch: (threshold: Float) -> Unit,
    onRemoveImage: () -> Unit,
    imageSize: Dp = 150.dp
){
    Row(
        modifier = Modifier.fillMaxWidth().background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ).padding(12.dp),

        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(imageSize)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            if (uri != null) {
                ImageDisplay(
                    uri = uri,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    type = MediaType.IMAGE
                )
                IconButton(
                    onClick = { onRemoveImage() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        .size(18.dp)
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove Image",
                        tint = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            } else {
                Column (
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Image icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize().weight(1f)
                    )
                }
            }
        }

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
                onClick = {onSearch(threshold) }
            ) {
                Icon(Icons.Default.ImageSearch, contentDescription = "Image search icon", modifier = Modifier.padding(end = 4.dp))
                Text(text = "Search")
            }
        }
    }
}