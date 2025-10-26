package com.fpf.smartscan.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpf.smartscan.ui.components.DirectoryPicker
import com.fpf.smartscan.R
import com.fpf.smartscan.ui.components.CustomSlider
import androidx.core.net.toUri
import com.fpf.smartscan.constants.SettingTypes
import com.fpf.smartscan.lib.getDownloadableModels
import com.fpf.smartscan.ui.components.ActionItem
import com.fpf.smartscan.ui.components.models.ModelManager
import com.fpf.smartscan.ui.components.models.ModelsList


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    type: String,
    viewModel: SettingsViewModel,
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val models by viewModel.importedModels.collectAsState()
    val context = LocalContext.current


    LaunchedEffect(Unit) {
        viewModel.event.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier.padding(16.dp).fillMaxSize()
    ) {
        Column {
            when (type) {
                SettingTypes.THRESHOLD -> {
                    CustomSlider(
                        label = stringResource(R.string.setting_similarity_threshold),
                        minValue = 0.18f,
                        maxValue = 0.28f,
                        initialValue = appSettings.similarityThreshold,
                        onValueChange = { value ->
                            viewModel.updateSimilarityThreshold(value)
                        },
                        description = stringResource(R.string.setting_similarity_threshold_description)
                    )
                }
                SettingTypes.MODELS -> {
                    ModelsList(
                        models = getDownloadableModels(context),
                        onDownload = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                    },
                    )
                }
                SettingTypes.MANAGE_MODELS -> {
                    ModelManager(models=models, onDelete = viewModel::onDeleteModel, onImport=viewModel::onImportModel)
                }
                SettingTypes.SEARCHABLE_IMG_DIRS -> {
                    DirectoryPicker(
                        directories = appSettings.searchableImageDirectories,
                        addDirectory = { newDir -> viewModel.addSearchableImageDirectory(newDir) },
                        deleteDirectory = { newDir -> viewModel.deleteSearchableImageDirectory(newDir) },
                        description = stringResource(R.string.setting_searchable_folders_description)
                    )
                }
                SettingTypes.SEARCHABLE_VID_DIRS -> {
                    DirectoryPicker(
                        directories = appSettings.searchableVideoDirectories,
                        addDirectory = { newDir -> viewModel.addSearchableVideoDirectory(newDir) },
                        deleteDirectory = { newDir -> viewModel.deleteSearchableVideoDirectory(newDir) },
                        description = stringResource(R.string.setting_searchable_folders_description)
                    )
                }
                SettingTypes.BACKUP_RESTORE -> {
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                        uri?.let { selectedUri ->
                            context.contentResolver.takePersistableUriPermission(
                                selectedUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            viewModel.restore(uri)
                        }
                    }
                    val backupDescription = "${stringResource(R.string.setting_backup_restore_description, "Export")}. ${stringResource(R.string.setting_backup_extra_description)}."
                        
                    ActionItem(
                        text = stringResource(id = R.string.setting_backup),
                        description = backupDescription,
                        onClick = { viewModel.backup() },
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
                        onClick = { launcher.launch(arrayOf("application/zip")) },
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
                else -> {}
            }
        }
    }
}
