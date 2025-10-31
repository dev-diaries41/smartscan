package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fpf.smartscan.R
import com.fpf.smartscan.data.SortOption

@Composable
fun SortDropdown(
    currentSortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = stringResource(R.string.sort_by)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(getSortOptionLabel(option)) },
                    onClick = {
                        onSortOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun getSortOptionLabel(option: SortOption): String {
    return when (option) {
        SortOption.SIMILARITY -> stringResource(R.string.sort_similarity)
        SortOption.DATE_NEWEST -> stringResource(R.string.sort_date_newest)
        SortOption.DATE_OLDEST -> stringResource(R.string.sort_date_oldest)
        SortOption.NAME_ASC -> stringResource(R.string.sort_name_asc)
        SortOption.NAME_DESC -> stringResource(R.string.sort_name_desc)
        SortOption.FOLDER -> stringResource(R.string.sort_folder)
    }
}
