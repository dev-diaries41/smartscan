package com.fpf.smartscan.ui.components.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.smartScanModelTypeOptions
import com.fpf.smartscan.data.DownloadableModel


@Composable
fun DownloadableModelCard(
    data: DownloadableModel,
    onDownload: (url: String) -> Unit,
) {
    var isDownloadAlertVisible by remember { mutableStateOf(false) }


    if(isDownloadAlertVisible){
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.download_model_alert_title)) },
            text = { Text(stringResource(R.string.download_model_alert_description)) },
            dismissButton = {
                TextButton(onClick = {
                    isDownloadAlertVisible = false
                }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    isDownloadAlertVisible = false
                    onDownload(data.url)
                }) {
                    Text("OK")
                }
            }
        )
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
                    modifier = Modifier.padding(end = 8.dp).size(32.dp),
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
                Button(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = { isDownloadAlertVisible = true }
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download icon",
                        modifier = Modifier.padding(end = 4.dp).size(16.dp)
                    )
                    Text(text = "Download", fontSize = 12.sp)
                }
            }
        }
    }
}