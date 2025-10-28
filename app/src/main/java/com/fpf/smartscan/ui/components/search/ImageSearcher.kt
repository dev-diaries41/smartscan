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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.ui.components.media.ImageUploader
import com.fpf.smartscan.ui.components.SelectorItem


@Composable
fun ImageSearcher(
    uri: Uri?,
    threshold: Float,
    mediaType: MediaType,
    searchEnabled: Boolean,
    mediaTypeSelectorEnabled: Boolean,
    hasCroppedImage: Boolean = false,
    onImageSelected: (Uri?) -> Unit,
    onMediaTypeChange: (type: MediaType) -> Unit,
    onSearch: (threshold: Float) -> Unit,
    onCropClick: () -> Unit = {},
    onClearCrop: () -> Unit = {},
){
    Column(
        modifier = Modifier.fillMaxWidth().background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Horní řádek - obrázek a ovládací prvky
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ImageUploader(
                uri = uri,
                onImageSelected = onImageSelected,
                placeholderText = {
                    Text(
                        text = stringResource(R.string.upload_image),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.alpha(0.8f).padding(8.dp),
                    )
                }
            )

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                SelectorItem(
                    enabled = mediaTypeSelectorEnabled,
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
                    enabled = searchEnabled,
                    onClick = { onSearch(threshold) }
                ) {
                    Icon(Icons.Default.ImageSearch, contentDescription = "Image search icon", modifier = Modifier.padding(end = 4.dp))
                    Text(text = "Search")
                }
            }
        }

        // Dolní řádek - crop tlačítka (zobrazí se pouze pokud je vybraný obrázek)
        if (uri != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCropClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Crop,
                        contentDescription = stringResource(R.string.action_crop),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = if (hasCroppedImage)
                            stringResource(R.string.action_recrop)
                        else
                            stringResource(R.string.action_crop)
                    )
                }

                // Tlačítko pro vymazání crop, pokud existuje cropped obrázek
                if (hasCroppedImage) {
                    Button(
                        onClick = onClearCrop,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_clear_crop),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(stringResource(R.string.action_clear_crop))
                    }
                }
            }
        }
    }
}