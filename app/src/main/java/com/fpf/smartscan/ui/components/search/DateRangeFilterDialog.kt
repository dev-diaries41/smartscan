package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
        initialSelectedEndDateMillis = currentEndDate,
        initialDisplayMode = DisplayMode.Picker
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Filtrovat podle data",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Date range picker - bez title, má vlastní header
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.fillMaxWidth(),
                    showModeToggle = false,
                    title = null,
                    headline = null
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
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

                    // Cancel button
                    TextButton(onClick = onDismiss) {
                        Text("Zrušit")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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
            }
        }
    }
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
