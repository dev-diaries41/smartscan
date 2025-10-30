package com.fpf.smartscan.ui.components.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.data.fewshot.FewShotPrototypeEntity
import com.fpf.smartscan.data.tags.UserTagEntity

/**
 * Společná rozbalovací sekce pro všechny filtry
 *
 * Obsahuje:
 * - Tagy (TagFilterChips)
 * - Few-Shot prototypes (FewShotSelector)
 * - Date range filter (DateRangeFilterButton)
 *
 * Všechny filtry jsou defaultně skryté v rozbalovacím seznamu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    // Tags
    availableTags: List<Pair<UserTagEntity, Int>>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    // Few-Shot
    availableFewShotPrototypes: List<FewShotPrototypeEntity>,
    selectedFewShotPrototype: FewShotPrototypeEntity?,
    onPrototypeSelected: (FewShotPrototypeEntity?) -> Unit,
    onFewShotSearchTriggered: () -> Unit,
    // Date range
    dateRangeStart: Long?,
    dateRangeEnd: Long?,
    onDateRangeClick: () -> Unit,
    onDateRangeClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pokud nejsou žádné filtry k dispozici, nezobrazovat sekci
    val hasAnyFilters = availableTags.isNotEmpty() ||
                        availableFewShotPrototypes.isNotEmpty() ||
                        dateRangeStart != null ||
                        dateRangeEnd != null

    if (!hasAnyFilters) return

    var isExpanded by remember { mutableStateOf(false) }

    // Počet aktivních filtrů
    val activeFiltersCount = selectedTags.size +
                            (if (selectedFewShotPrototype != null) 1 else 0) +
                            (if (dateRangeStart != null || dateRangeEnd != null) 1 else 0)

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
                        text = "Filtry:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (activeFiltersCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = activeFiltersCount.toString(),
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

        // Rozbalovací obsah s animací
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Tagy
                if (availableTags.isNotEmpty()) {
                    TagFilterChips(
                        availableTags = availableTags,
                        selectedTags = selectedTags,
                        onTagToggle = onTagToggle
                    )
                }

                // 2. Few-Shot
                if (availableFewShotPrototypes.isNotEmpty()) {
                    FewShotSelector(
                        prototypes = availableFewShotPrototypes,
                        selectedPrototype = selectedFewShotPrototype,
                        onPrototypeSelected = onPrototypeSelected,
                        onSearchTriggered = onFewShotSearchTriggered
                    )
                }

                // 3. Date range
                DateRangeFilterButton(
                    currentStartDate = dateRangeStart,
                    currentEndDate = dateRangeEnd,
                    onClick = onDateRangeClick,
                    onClear = onDateRangeClear,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Tlačítko pro filtrování podle rozsahu dat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilterButton(
    currentStartDate: Long?,
    currentEndDate: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasFilter = currentStartDate != null || currentEndDate != null
    val description = getDateRangeDescription(currentStartDate, currentEndDate)

    Row(
        modifier = modifier
            .wrapContentWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = hasFilter,
            onClick = onClick,
            label = {
                Text(
                    text = if (hasFilter) description else "Filtrovat podle data"
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Kalendář"
                )
            },
            trailingIcon = if (hasFilter) {
                {
                    IconButton(
                        onClick = { onClear() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Vymazat filter",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null
        )
    }
}
