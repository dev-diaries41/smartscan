package com.fpf.smartscan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun CustomSlider(
    label: String,
    minValue: Float,
    maxValue: Float,
    initialValue: Float = minValue,
    onValueChange: (Float) -> Unit,
    format: (Float) -> String = { "%.2f".format(it) },
    description: String? = null,
    ) {
    var sliderValue by remember { mutableFloatStateOf(initialValue) }

    Column {
        Text(text = "$label: ${format(sliderValue)}", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = minValue..maxValue,
            steps = 0,  // Continuous values for fine-grained control
            modifier = Modifier.padding(top = 8.dp).height(24.dp)
        )

        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.8f).padding(bottom = 16.dp),
            )
        }
    }
}
