package com.fpf.smartscan.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.constants.smartScanModelTypesOptions

@Composable
fun ModelImporter(
    addModel: (uri: Uri, type: String) -> Unit,
    selectModelType: (String) -> Unit,
    description: String? = null,
) {
    var selectedModelType by remember { mutableStateOf("") }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { selectedUri ->
            context.contentResolver.takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addModel(uri, selectedModelType)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.8f).padding(bottom = 16.dp),
            )
        }


        SelectorItem(
            label = "Model type",
            options = smartScanModelTypesOptions.values.toList(),
            selectedOption = selectedModelType,
            onOptionSelected = { selected ->
                val newMode = smartScanModelTypesOptions.entries
                    .find { it.value == selected }
                    ?.key ?: ""
                selectModelType(newMode)
            }
        )

        Button(onClick = { launcher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-tflite")) }) {
            Icon(Icons.Default.FileUpload, contentDescription = "Import")
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Import model")
        }
    }
}
