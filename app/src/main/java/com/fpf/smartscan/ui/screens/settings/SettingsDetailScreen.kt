package com.fpf.smartscan.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            when (type) {
                "targets" -> {
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
                "destinations" -> {
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
                "threshold" -> {
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

                "organiserAccuracy" -> {
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
                else -> {
                    Text("Unknown setting type")
                }
            }
        }
    }
}
