package com.fpf.smartscan.ui.components.models

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.data.ImportedModel

@Composable
fun ModelManager(
    models: List<ImportedModel>,
    onImport: (uri: Uri) -> Unit,
    onDelete: (model: ImportedModel) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { selectedUri ->
            context.contentResolver.takePersistableUriPermission(
                selectedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onImport(uri)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launcher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-tflite")) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = shapes.extraLarge
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Import model"
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.verticalScroll(scrollState).fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding() + 80.dp)) {
            if (models.isEmpty()) {
                Text(
                    text = "No models to manage",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.alpha(0.8f).padding(vertical = 16.dp),
                )
            } else {
                models.forEach { model -> ImportedModelCard(data = model, onDelete = { onDelete(model) }) }
            }
        }
    }
}
