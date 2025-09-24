package com.fpf.smartscan.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.fpf.smartscan.lib.getDirectoryName

@Composable
fun DirectoryPicker(
    directories: List<String>,
    addDirectory: (String) -> Unit,
    deleteDirectory: (String) -> Unit,
    description: String? = null,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { selectedUri ->
            context.contentResolver.takePersistableUriPermission(
                selectedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            if (!directories.contains(selectedUri.toString())) {
                addDirectory(selectedUri.toString())
            }
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


        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = { launcher.launch(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add folder")
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Add Folder")
            }
        }

        if (directories.isEmpty()) {
            Text(
                text = "No directories selected.",
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
                    directories.forEach { dir ->
                        val displayName = getDirectoryName(context, dir.toUri())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = displayName, modifier = Modifier.weight(1f))
                            IconButton(onClick = { deleteDirectory(dir) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove directory", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
