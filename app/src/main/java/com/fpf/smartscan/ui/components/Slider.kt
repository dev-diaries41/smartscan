package com.fpf.smartscan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun CustomSlider(
    minValue: Float,
    maxValue: Float,
    initialValue: Float = minValue,
    onValueChange: (Float) -> Unit,
    description: String? = null
) {
    var sliderValue by remember { mutableStateOf(initialValue) }

    Column (modifier = Modifier.padding(16.dp)) {
        Text(text = "Similarity Threshold: ${"%.2f".format(sliderValue)}")

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = minValue..maxValue,
            steps = 0,  // Continuous values for fine-grained control
            modifier = Modifier.padding(top = 8.dp)
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
