package com.fpf.smartscan.ui.components.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.data.tags.UserTagEntity

/**
 * Rozbalovací seznam s filter chips pro tagy
 *
 * Zobrazuje dostupné tagy s počtem obrázků a umožňuje jejich výběr/deselect
 * pro filtrování výsledků vyhledávání. Seznam je defaultně sbalený.
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

    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Klikací header pro rozbalení/sbalení
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tagy:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedTags.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = selectedTags.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "Sbalit" else "Rozbalit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Rozbalovací seznam tagů s animací
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableTags.forEach { (tag, count) ->
                    TagFilterChip(
                        tag = tag,
                        count = count,
                        isSelected = selectedTags.contains(tag.name),
                        onClick = { onTagToggle(tag.name) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
