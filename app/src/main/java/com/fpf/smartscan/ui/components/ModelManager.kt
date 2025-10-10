package com.fpf.smartscan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.MODEL_DIR
import com.fpf.smartscan.constants.smartScanModelTypeOptions
import com.fpf.smartscan.data.SmartScanModelType
import java.io.File

@Composable
fun ModelManager(
    description: String? = null,
) {
    var isDeleteAlertVisible by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var modelsFiles by remember { mutableStateOf(emptyList<File>()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val modelsDir = File(context.filesDir, MODEL_DIR)
        if(!modelsDir.exists()) modelsDir.mkdirs()
        modelsDir.listFiles()?.let{modelsFiles = it.toList()}
    }

    if(isDeleteAlertVisible && fileToDelete != null){
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.delete_model_alert_title)) },
            text = { Text(stringResource(R.string.delete_model_alert_description)) },
            dismissButton = {
                TextButton(onClick = {
                    isDeleteAlertVisible = false
                }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    isDeleteAlertVisible = false
                    fileToDelete!!.delete()
                    modelsFiles = modelsFiles - fileToDelete!!
                    fileToDelete = null
                }) {
                    Text("OK")
                }
            }
        )
    }

    Column() {
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.8f).padding(bottom = 16.dp),
            )
        }

        if (modelsFiles.isEmpty()) {
            Text(
                text = "No models.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.5f).padding(vertical = 16.dp),
            )
        } else {
            Card (
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors= CardDefaults.cardColors(Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    modelsFiles.forEach { file ->
                        val name =  SmartScanModelType.entries.first { type -> file.name.contains(type.tag) }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = "Model icon", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = smartScanModelTypeOptions[name]!!, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                isDeleteAlertVisible = true
                                fileToDelete = file
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete model", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
