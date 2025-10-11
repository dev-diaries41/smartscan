package com.fpf.smartscan.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Scaffold
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
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launcher.launch(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = shapes.extraLarge
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add directory")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.verticalScroll(scrollState).fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding() + 80.dp)) {
            if (directories.isEmpty()) {
                Text(
                    text = "No directories selected",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.alpha(0.5f).padding(vertical = 16.dp),
                )
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.alpha(0.8f).padding(bottom = 16.dp),
                )
            }
            if (directories.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    directories.forEach { dir ->
                        DirectoryCard(directoryName = getDirectoryName(context, dir.toUri()), onDelete = { deleteDirectory(dir) })
                    }
                }
            }
        }
    }
}

@Composable
fun DirectoryCard(directoryName: String, onDelete: () -> Unit){
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = directoryName, modifier = Modifier.weight(1f))
        IconButton(onClick = { onDelete() }) {
            Icon(Icons.Default.Delete, contentDescription = "Remove directory", tint = Color.Red)
        }
    }
}