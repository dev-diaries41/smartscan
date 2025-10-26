package com.fpf.smartscan.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.R

@Composable
fun BackupAndRestore(
    onRestore: (uri: Uri) -> Unit,
    onBackup:(uri: Uri) -> Unit,
    backupFilename: String
){
    val context = LocalContext.current

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { selectedUri ->
            context.contentResolver.takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onRestore(selectedUri)
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            context.contentResolver.takePersistableUriPermission(
                fileUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            onBackup(fileUri)
        }
    }

    ActionItem(
        text = stringResource(id = R.string.setting_backup),
        description = stringResource(R.string.setting_backup_restore_description, "Export"),
        onClick = { backupLauncher.launch(backupFilename) },
        buttonContent = { enabled, onClick ->
            Button(
                modifier = Modifier.padding(horizontal = 8.dp),
                enabled=enabled,
                onClick = { onClick() }
            ) {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = "Backup icon",
                    modifier = Modifier.padding(end = 4.dp).size(16.dp)
                )
                Text(text = "Backup", fontSize = 12.sp)
            }
        }
    )
    ActionItem(
        text = stringResource(id = R.string.setting_restore),
        description = stringResource(R.string.setting_backup_restore_description, "Import"),
        onClick = {restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
        buttonContent = { enabled, onClick ->
            Button(
                modifier = Modifier.padding(horizontal = 8.dp),
                enabled=enabled,
                onClick = { onClick() }
            ) {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = "Restore icon",
                    modifier = Modifier.padding(end = 4.dp).size(16.dp)
                )
                Text(text = "Restore", fontSize = 12.sp)
            }
        }

    )
}