package com.fpf.smartscan.ui.components


import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.Boolean

@Composable
fun SettingsCard(
    text: String,
    onClick: () -> Unit,
    description: String? = null,
    enabled: Boolean = true
) {
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5F)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = textColor)
            IconButton(
                enabled=enabled,
                onClick=onClick,
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Open screen to update setting")
            }
        }
        if (description != null) {
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = textColor, modifier = Modifier.fillMaxWidth(0.7f))
        }
    }
}

@Composable
fun SettingsSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
    description: String? = null,
    enabled: Boolean = true
) {
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5F)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = textColor)
            Switch(
                enabled=enabled,
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Color.Green)
            )
        }
        if (description != null) {
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = textColor)
        }
    }
}

@Composable
fun SettingsIncrementor(
    label: String,
    value: String,
    description: String? = null,
    enabled: Boolean = true,
    minValue: Int = 0,
    maxValue: Int = Int.MAX_VALUE,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(140.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(percent = 50)
                    ),
            ){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        if ((value.toIntOrNull() ?: 0) > minValue) onDecrement()
                    }) {
                        Icon(Icons.Default.Remove,
                            contentDescription = "Decrement",
                        )
                    }

                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = textColor,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                    )

                    IconButton(onClick = {
                        if ((value.toIntOrNull() ?: 0) < maxValue) onIncrement()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Increment")
                    }
                }
            }


            if (description != null) {
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = textColor
                )
            }
        }
    }
}


@Composable
fun SettingsTextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    description: String? = null,
    enabled: Boolean = true,
    isNumeric: Boolean = false
) {
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5F)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = textColor)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                enabled = enabled,
                modifier = Modifier
                    .width(80.dp),
                keyboardOptions = if (isNumeric) {
                    KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                } else {
                    KeyboardOptions.Default
                }
            )
            if (description != null) {
                Text(text = description, fontSize = 12.sp, color = textColor)
            }
        }
    }
}


@Composable
fun SettingsSelect(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    description: String? = null,
    enabled: Boolean = true
) {
    val textColor = if (enabled)
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
            OutlinedButton(
                onClick = { showDialog = true },
                enabled = enabled,
                modifier = Modifier.widthIn(max = 140.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedOption.isEmpty()) "Select option" else selectedOption,
                        color = if (selectedOption.isEmpty()) Color.Gray else textColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                    )
                }
            }

        }
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = label) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected(option)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = option == selectedOption,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = option)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton (onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



