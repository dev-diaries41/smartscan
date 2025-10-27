package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog pro výběr rozsahu datumů pomocí dvou samostatných Material3 DatePicker komponent
 *
 * Umožňuje uživateli vybrat datum od a datum do odděleně, což je vhodnější pro dlouhé rozsahy.
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
    var startDate by remember { mutableStateOf(currentStartDate) }
    var endDate by remember { mutableStateOf(currentEndDate) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = "Filtrovat podle data vytvoření",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Start Date TextField
                OutlinedTextField(
                    value = startDate?.let { formatDate(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Datum od") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Vybrat datum od")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true,
                    placeholder = { Text("Vyberte datum") }
                )

                // End Date TextField
                OutlinedTextField(
                    value = endDate?.let { formatDate(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Datum do") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Vybrat datum do")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Vyberte datum") }
                )

                // Buttons pro otevření pickerů
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vybrat datum od")
                    }
                    TextButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vybrat datum do")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Clear button (pouze pokud je něco vybrané)
                    if (startDate != null || endDate != null) {
                        TextButton(
                            onClick = {
                                startDate = null
                                endDate = null
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
                            onConfirm(startDate, endDate)
                            onDismiss()
                        },
                        enabled = startDate != null || endDate != null
                    ) {
                        Text("Použít")
                    }
                }
            }
        }
    }

    // Start Date Picker Modal
    if (showStartPicker) {
        DatePickerModal(
            title = "Vyberte datum od",
            initialDateMillis = startDate,
            onDismiss = { showStartPicker = false },
            onDateSelected = { selected ->
                startDate = selected
                showStartPicker = false
            },
            selectableDates = null
        )
    }

    // End Date Picker Modal - s validací že end >= start
    if (showEndPicker) {
        DatePickerModal(
            title = "Vyberte datum do",
            initialDateMillis = endDate,
            onDismiss = { showEndPicker = false },
            onDateSelected = { selected ->
                endDate = selected
                showEndPicker = false
            },
            selectableDates = if (startDate != null) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= startDate!!
                    }
                }
            } else null
        )
    }
}

/**
 * Modální dialog s DatePicker komponentou
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    title: String,
    initialDateMillis: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long?) -> Unit,
    selectableDates: SelectableDates?
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = selectableDates ?: @OptIn(ExperimentalMaterial3Api::class)
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            showModeToggle = false
        )
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
