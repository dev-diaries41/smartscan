package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.data.tags.ImageTagEntity
import com.fpf.smartscan.ui.screens.tags.TagViewModel

/**
 * Komponenta pro zobrazení tagů přiřazených k obrázku v Media Viewer
 *
 * Zobrazuje:
 * - Chipy s názvy tagů a confidence (%)
 * - Tlačítko pro přidání nového tagu
 * - Možnost odebrání tagu kliknutím na X ikonu
 *
 * @param imageId MediaStore ID obrázku
 * @param tagViewModel ViewModel pro práci s tagy
 * @param onAddTag Callback při kliknutí na "Přidat tag"
 * @param onRemoveTag Callback při odebrání tagu
 */
@Composable
fun TagChipsRow(
    imageId: Long,
    tagViewModel: TagViewModel = viewModel(),
    onAddTag: (Long) -> Unit,
    onRemoveTag: (Long, String) -> Unit
) {
    val imageTags by tagViewModel.getTagsForImage(imageId).collectAsState(initial = emptyList())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zobrazení existujících tagů
        imageTags.forEach { imageTag ->
            TagChip(
                imageTag = imageTag,
                onRemove = { onRemoveTag(imageId, imageTag.tagName) }
            )
        }

        // Tlačítko pro přidání nového tagu
        AssistChip(
            onClick = { onAddTag(imageId) },
            label = { Text("Přidat tag") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Přidat tag"
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

/**
 * Samostatný chip pro jeden tag
 */
@Composable
private fun TagChip(
    imageTag: ImageTagEntity,
    onRemove: () -> Unit
) {
    val confidencePercent = (imageTag.confidence * 100).toInt()
    val label = if (imageTag.isUserAssigned) {
        // Manuálně přiřazené tagy nemají confidence
        imageTag.tagName
    } else {
        // Auto-tagging tagy zobrazují confidence
        "${imageTag.tagName} ($confidencePercent%)"
    }

    AssistChip(
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Odebrat tag",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (imageTag.isUserAssigned) {
                // Manuální tagy mají primární barvu
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                // Auto tagy mají neutrální barvu
                MaterialTheme.colorScheme.surfaceVariant
            },
            labelColor = if (imageTag.isUserAssigned) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    )
}
