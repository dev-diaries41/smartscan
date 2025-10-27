package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog pro výběr rozsahu datumů pomocí Material3 DateRangePicker
 *
 * Umožňuje uživateli vybrat from-to datum pro filtrování výsledků vyhledávání.
 *
 * @param currentStartDate Aktuálně vybraný start datum (timestamp v ms) nebo null
 * @param currentEndDate Aktuálně vybraný end datum (timestamp v ms) nebo null
 * @param onDismiss Callback při zavření dialogu
 * @param onConfirm Callback při potvrzení s vybranými daty (startDate, endDate)
 * @param onClear Callback pro vymazání filtru
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterDialog(
    currentStartDate: Long?,
    currentEndDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit,
    onClear: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = currentStartDate,
        initialSelectedEndDateMillis = currentEndDate
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filtrovat podle data",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                // Zobrazení aktuálního výběru
                if (dateRangePickerState.selectedStartDateMillis != null ||
                    dateRangePickerState.selectedEndDateMillis != null) {

                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

                    Text(
                        text = "Vybraný rozsah:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val startText = dateRangePickerState.selectedStartDateMillis?.let {
                        dateFormat.format(Date(it))
                    } ?: "?"

                    val endText = dateRangePickerState.selectedEndDateMillis?.let {
                        dateFormat.format(Date(it))
                    } ?: "?"

                    Text(
                        text = "$startText - $endText",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Date range picker
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row {
                // Clear button (pouze pokud je něco vybrané)
                if (currentStartDate != null || currentEndDate != null) {
                    TextButton(
                        onClick = {
                            onClear()
                            onDismiss()
                        }
                    ) {
                        Text("Vymazat")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Apply button
                TextButton(
                    onClick = {
                        onConfirm(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis
                        )
                        onDismiss()
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null ||
                              dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("Použít")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

/**
 * Formátuje timestamp na čitelné datum
 */
fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return ""
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

/**
 * Vytvoří popis date range filtru pro zobrazení v UI
 */
fun getDateRangeDescription(startDate: Long?, endDate: Long?): String {
    return when {
        startDate != null && endDate != null -> {
            "${formatDate(startDate)} - ${formatDate(endDate)}"
        }
        startDate != null -> {
            "Od ${formatDate(startDate)}"
        }
        endDate != null -> {
            "Do ${formatDate(endDate)}"
        }
        else -> ""
    }
}
