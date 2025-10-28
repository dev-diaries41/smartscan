package com.fpf.smartscan.ui.components.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.fpf.smartscan.R
import com.fpf.smartscan.data.tags.MediaTagEntity
import com.fpf.smartscan.data.tags.UserTagEntity

/**
 * Dialog pro výběr tagu, který se má přidat k obrázku
 *
 * Zobrazuje seznam všech dostupných tagů, které ještě nejsou přiřazené k obrázku.
 * Tagy, které už jsou přiřazené, jsou skryté (filtrované).
 *
 * @param imageId MediaStore ID obrázku
 * @param availableTags Seznam všech user-defined tagů
 * @param currentTags Seznam tagů už přiřazených k tomuto obrázku
 * @param onDismiss Callback při zavření dialogu
 * @param onTagSelected Callback při výběru tagu
 */
@Composable
fun TagPickerDialog(
    imageId: Long,
    availableTags: List<UserTagEntity>,
    currentTags: List<MediaTagEntity>,
    onDismiss: () -> Unit,
    onTagSelected: (UserTagEntity) -> Unit
) {
    // Filtrovat tagy, které už jsou přiřazené
    val currentTagNames = currentTags.map { it.tagName }.toSet()
    val selectableTags = availableTags.filter { tag ->
        !currentTagNames.contains(tag.name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Přidat tag",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            if (selectableTags.isEmpty()) {
                // Žádné dostupné tagy
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Žádné další tagy k dispozici",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Všechny tagy jsou už přiřazené nebo nemáte vytvořené žádné tagy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Seznam tagů
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(selectableTags) { tag ->
                        TagPickerItem(
                            tag = tag,
                            onClick = {
                                onTagSelected(tag)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Jednotlivá položka v tag pickeru
 */
@Composable
private fun TagPickerItem(
    tag: UserTagEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barevný indikátor
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = Color(tag.color)
        ) {
            // Prázdný, pouze barva
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Název a popis
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            // Zobrazit popis (zkrácený)
            if (tag.description.isNotBlank()) {
                Text(
                    text = tag.description.take(60) + if (tag.description.length > 60) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Ikona pro aktivní tagy
        if (tag.isActive) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Aktivní tag",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
