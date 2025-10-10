package com.fpf.smartscan.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.constants.smartScanModelTypeOptions
import com.fpf.smartscan.data.DownloadableModel
import com.fpf.smartscan.data.SmartScanModelType

@Composable
fun ModelsList(
    models: List<DownloadableModel>,
    onDownload: (url: String) -> Unit,
    onImport: (uri: Uri, type: SmartScanModelType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        models.forEach { item ->
            ModelCard(data = item, onDownload = onDownload, onImport = onImport)
        }
    }
}

@Composable
fun ModelCard(
    data: DownloadableModel,
    onDownload: (url: String) -> Unit,
    onImport: (uri: Uri, type: SmartScanModelType) -> Unit
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { selectedUri ->
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onImport(uri, data.type)
            }
        }

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Model icon",
                    modifier = Modifier.padding(end = 16.dp).size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = data.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Type: ${smartScanModelTypeOptions[data.type]}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.8f)
                    )

                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onDownload(data.url) }
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download icon",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(text = "Download")
                }
                Button(
                    onClick = { launcher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-tflite")) }
                ) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = "Import icon",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(text = "Import")
                }
            }
        }
    }
}