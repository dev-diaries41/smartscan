package com.fpf.smartscan.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.fpf.smartscan.lib.getImportedModels
import com.fpf.smartscan.ui.components.ModelManager
import com.fpf.smartscan.ui.components.ModelsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    type: String,
    viewModel: SettingsViewModel,
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val context = LocalContext.current
    val initialTargetDirectories = remember { appSettings.targetDirectories }
    val initialDestinationDirectories = remember { appSettings.destinationDirectories }
    val initialOrganiserSimilarity = remember { appSettings.organiserSimilarityThreshold }
    val initialOrganiserConfMargin = remember { appSettings.organiserConfMargin }
    val scrollState = rememberScrollState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onSettingsDetailsExit(
                initialDestinationDirectories = initialDestinationDirectories,
                initialTargetDirectories = initialTargetDirectories,
                initialOrganiserSimilarity = initialOrganiserSimilarity,
                initialOrganiserConfMargin = initialOrganiserConfMargin
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.importEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }


    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            when (type) {
                SettingTypes.TARGETS -> {
                    DirectoryPicker(
                        directories = appSettings.targetDirectories,
                        addDirectory = { newDir ->
                            viewModel.addTargetDirectory(newDir)
                        },
                        deleteDirectory = { newDir ->
                            viewModel.deleteTargetDirectory(newDir)
                        },
                        description = stringResource(R.string.setting_target_folders_description)
                    )
                }
                SettingTypes.DESTINATIONS -> {
                    DirectoryPicker(
                        directories = appSettings.destinationDirectories,
                        addDirectory = { newDir ->
                            viewModel.addDestinationDirectory(newDir)
                        },
                        deleteDirectory = { newDir ->
                            viewModel.deleteDestinationDirectory(newDir)
                        },
                        description = stringResource(R.string.setting_destination_folders_description)
                    )
                }
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

                SettingTypes.ORGANISER_ACCURACY -> {
                    CustomSlider(
                        label = stringResource(R.string.setting_similarity_threshold),
                        minValue = 0.4f,
                        maxValue = 0.7f,
                        initialValue = appSettings.organiserSimilarityThreshold,
                        onValueChange = { value ->
                            viewModel.updateOrganiserSimilarityThreshold(value)
                        },
                        description = stringResource(R.string.setting_similarity_threshold_description)
                    )
                    CustomSlider(
                        label = stringResource(R.string.setting_organisation_conf_margin_title),
                        minValue = 0.01f,
                        maxValue = 0.05f,
                        initialValue = appSettings.organiserConfMargin,
                        onValueChange = { value ->
                            viewModel.updateOrganiserConfidenceMargin(value)
                        },
                        description = stringResource(R.string.setting_organisation_conf_margin_description)
                    )
                }

                SettingTypes.MODELS -> {
                    ModelsList(
                        models = getDownloadableModels(context),
                        onDownload = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                    },
                        onImport=viewModel::onImportModel
                    )
                }
                SettingTypes.MANAGE_MODELS -> {
                    ModelManager(importedModels = getImportedModels(context))
                }
                else -> {}
            }
        }
    }
}
