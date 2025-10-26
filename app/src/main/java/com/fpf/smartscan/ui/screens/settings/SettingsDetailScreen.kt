package com.fpf.smartscan.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.ui.components.DirectoryPicker
import com.fpf.smartscan.R
import com.fpf.smartscan.ui.components.CustomSlider
import androidx.core.net.toUri
import com.fpf.smartscan.constants.SettingTypes
import com.fpf.smartscan.lib.getDownloadableModels
import com.fpf.smartscan.ui.components.BackupAndRestore
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
                    BackupAndRestore(
                        onBackup = viewModel::backup,
                        onRestore = viewModel::restore
                    )
                }
                else -> {}
            }
        }
    }
}
