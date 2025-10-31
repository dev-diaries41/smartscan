package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.data.fewshot.FewShotPrototypeEntity

/**
 * Zobrazení aktivních filtrů jako chips pod search barem
 *
 * Design podle UI mockupu:
 * - Horizontální scrollable Row
 * - AssistChip s ikonami podle typu filtru
 * - Close button (×) pro rychlé odstranění
 * - Zobrazuje se pouze když jsou nějaké filtry aktivní
 */
@Composable
fun ActiveFiltersChips(
    selectedTags: Set<String>,
    onTagRemove: (String) -> Unit,
    selectedFewShotPrototype: FewShotPrototypeEntity?,
    onFewShotRemove: () -> Unit,
    dateRangeStart: Long?,
    dateRangeEnd: Long?,
    onDateRangeRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Počet aktivních filtrů
    val hasActiveTags = selectedTags.isNotEmpty()
    val hasFewShot = selectedFewShotPrototype != null
    val hasDateRange = dateRangeStart != null || dateRangeEnd != null
    val hasAnyFilters = hasActiveTags || hasFewShot || hasDateRange

    // Nezobrazovat, pokud nejsou žádné aktivní filtry
    if (!hasAnyFilters) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Tag chips
        selectedTags.forEach { tagName ->
            AssistChip(
                onClick = { onTagRemove(tagName) },
                label = { Text(tagName) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_delete),
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    trailingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }

        // 2. Few-Shot chip
        if (selectedFewShotPrototype != null) {
            AssistChip(
                onClick = { onFewShotRemove() },
                label = { Text(stringResource(R.string.fewshot_label_prefix, selectedFewShotPrototype.name)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_delete),
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    trailingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }

        // 3. Date range chip
        if (hasDateRange) {
            val dateDescription = getDateRangeDescription(dateRangeStart, dateRangeEnd)
            AssistChip(
                onClick = { onDateRangeRemove() },
                label = { Text(dateDescription) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_delete),
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    trailingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}
