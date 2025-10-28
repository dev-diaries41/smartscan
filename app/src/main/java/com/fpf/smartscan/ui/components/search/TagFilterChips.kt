package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.data.tags.UserTagEntity

/**
 * Vertikální seznam s filter chips pro tagy
 *
 * Zobrazuje dostupné tagy s počtem obrázků a umožňuje jejich výběr/deselect
 * pro filtrování výsledků vyhledávání.
 *
 * @param availableTags List párů (UserTagEntity, počet obrázků s tímto tagem)
 * @param selectedTags Set názvů vybraných tagů
 * @param onTagToggle Callback při kliknutí na tag
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFilterChips(
    availableTags: List<Pair<UserTagEntity, Int>>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableTags.isEmpty()) {
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filtry:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedTags.isNotEmpty()) {
                Text(
                    text = "${selectedTags.size} ${if (selectedTags.size == 1) "vybraný" else if (selectedTags.size < 5) "vybrané" else "vybraných"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Vertikální seznam chips - omezen na max 3 řádky
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            availableTags.take(5).forEach { (tag, count) ->
                TagFilterChip(
                    tag = tag,
                    count = count,
                    isSelected = selectedTags.contains(tag.name),
                    onClick = { onTagToggle(tag.name) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Indikace více tagů pokud je jich víc než 5
            if (availableTags.size > 5) {
                Text(
                    text = "+${availableTags.size - 5} dalších",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Jednotlivý filter chip pro tag
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagFilterChip(
    tag: UserTagEntity,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(tag.color))
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )

                // Tag name
                Text(tag.name)

                // Count badge
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(tag.color).copy(alpha = 0.8f),
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
