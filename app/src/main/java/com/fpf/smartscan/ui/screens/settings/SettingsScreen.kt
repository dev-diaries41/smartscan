package com.fpf.smartscan.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.R
import com.fpf.smartscan.ui.components.ActionItem
import com.fpf.smartscan.ui.components.SelectorItem
import com.fpf.smartscan.ui.components.SwitchItem
import androidx.core.net.toUri
import com.fpf.smartscan.constants.Routes
import com.fpf.smartscan.constants.SettingTypes
import com.fpf.smartscan.constants.colorSchemeDisplayNames
import com.fpf.smartscan.constants.themeModeDisplayNames
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.services.refreshIndex
import com.fpf.smartscan.ui.components.CustomSlider
import com.fpf.smartscan.ui.permissions.StorageAccess
import com.fpf.smartscan.ui.permissions.getStorageAccess
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current // Access the current context
    val sourceCodeUrl = stringResource(R.string.source_code_url)
    val redditUrl = stringResource(R.string.reddit_url)
    val versionName: String? = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: Exception) {
        null
    }

    val refreshMessageFull = stringResource(R.string.setting_refresh_index_description_full)
    val refreshMessagePartial = stringResource(R.string.setting_refresh_index_description_partial)
    val refreshMessageDenied = stringResource(R.string.setting_refresh_index_description_denied)

    var showRefreshImageIndexDialog by remember { mutableStateOf(false) }
    var showRefreshVideoIndexDialog by remember { mutableStateOf(false) }
    var refreshDialogMessage by remember { mutableStateOf("") }
    var currentStorageAccess by remember { mutableStateOf(StorageAccess.Full) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showRefreshImageIndexDialog || showRefreshVideoIndexDialog) {
            AlertDialog(
                onDismissRequest = {
                    if(showRefreshImageIndexDialog) {
                    showRefreshImageIndexDialog = false
                }else{
                        showRefreshVideoIndexDialog = false
                    }
                                   },
                title = { Text(text = if (showRefreshImageIndexDialog) {
                    stringResource(id = R.string.setting_refresh_image_index)
                } else {
                    stringResource(id = R.string.setting_refresh_video_index)
                }) },
                text = { Text(text = refreshDialogMessage) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (currentStorageAccess != StorageAccess.Denied) {
                                if(showRefreshImageIndexDialog){
                                    refreshIndex(context.applicationContext,
                                        MediaIndexForegroundService.TYPE_IMAGE)
                                }else{
                                    refreshIndex(context.applicationContext,
                                        MediaIndexForegroundService.TYPE_VIDEO)
                                }
                            }
                            if(showRefreshImageIndexDialog){
                                showRefreshImageIndexDialog = false
                            }else{
                                showRefreshVideoIndexDialog = false
                            }
                        }
                    ) {
                        Text(text = "Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if(showRefreshImageIndexDialog){
                            showRefreshImageIndexDialog = false
                        }else{
                            showRefreshVideoIndexDialog = false
                        } }
                    ) {
                        Text(text = "Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = stringResource(id = R.string.general_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                SelectorItem(
                    label = stringResource(id = R.string.setting_theme),
                    options = themeModeDisplayNames.values.toList(),
                    selectedOption = themeModeDisplayNames[appSettings.theme]!!,
                    onOptionSelected = { selected ->
                        val theme = themeModeDisplayNames.entries.find { it.value == selected }?.key ?: ThemeMode.SYSTEM
                        viewModel.updateTheme(theme)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                SelectorItem(
                    label = stringResource(id = R.string.setting_color),
                    options = colorSchemeDisplayNames.values.toList(),
                    selectedOption = colorSchemeDisplayNames[appSettings.color]!!,
                    onOptionSelected = { selected ->
                        val color = colorSchemeDisplayNames.entries.find { it.value == selected }?.key ?: ColorSchemeType.SMARTSCAN
                        viewModel.updateColorScheme(color)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.search_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                CustomSlider(
                    label = stringResource(id = R.string.setting_max_search_results),
                    minValue = 1f,
                    maxValue = 100f,
                    initialValue = appSettings.maxSearchResults.toFloat(),
                    onValueChange = { value ->
                        viewModel.updateMaxSearchResults(value.toInt())
                    },
                    format = { "%.0f".format(it) }
                )
                CustomSlider(
                    label = stringResource(id = R.string.setting_similarity_threshold),
                    minValue = 0.18f,
                    maxValue = 0.28f,
                    initialValue = appSettings.similarityThreshold,
                    onValueChange = { value ->
                        viewModel.updateSimilarityThreshold(value)
                    },
                    format = { "%.2f".format(it) },
                    description = stringResource(R.string.setting_similarity_threshold_description)
                )
                ActionItem(
                    text = stringResource(id = R.string.setting_refresh_image_index),
                    onClick = {
                        val storageAccess = getStorageAccess(context)
                        currentStorageAccess = storageAccess
                        refreshDialogMessage = when (storageAccess) {
                            StorageAccess.Full -> refreshMessageFull
                            StorageAccess.Partial -> refreshMessagePartial
                            StorageAccess.Denied -> refreshMessageDenied
                        }
                        showRefreshImageIndexDialog = true
                    }
                )

                ActionItem(
                    text = stringResource(id = R.string.setting_refresh_video_index),
                    onClick = {
                        val storageAccess = getStorageAccess(context)
                        currentStorageAccess = storageAccess
                        refreshDialogMessage = when (storageAccess) {
                            StorageAccess.Full -> refreshMessageFull
                            StorageAccess.Partial -> refreshMessagePartial
                            StorageAccess.Denied -> refreshMessageDenied
                        }
                        showRefreshVideoIndexDialog = true
                    }
                )
                ActionItem(
                    text = stringResource(id = R.string.setting_searchable_image_folders),
                    onClick = { onNavigate(Routes.settingsDetail(SettingTypes.SEARCHABLE_IMG_DIRS)) }
                )
                ActionItem(
                    text = stringResource(id = R.string.setting_searchable_video_folders),
                    onClick = { onNavigate(Routes.settingsDetail(SettingTypes.SEARCHABLE_VID_DIRS)) }
                )
                SelectorItem(
                    label = stringResource(id = R.string.setting_index_frequency),
                    options = listOf(
                        stringResource(id = R.string.scan_frequency_1d),
                        stringResource(id = R.string.scan_frequency_1w)
                    ),
                    selectedOption = appSettings.indexFrequency,
                    onOptionSelected = { selected ->
                        viewModel.updateIndexFrequency(selected)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.image_management_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                SwitchItem(
                    text = stringResource(id = R.string.setting_enable_scan),
                    checked = appSettings.enableScan,
                    onCheckedChange = { checked ->
                        viewModel.updateEnableScan(checked)
                    }
                )
                ActionItem(
                    enabled = appSettings.enableScan,
                    text = stringResource(id = R.string.setting_target_folders),
                    onClick = { onNavigate(Routes.settingsDetail(SettingTypes.TARGETS)) }
                )
                ActionItem(
                    enabled = appSettings.enableScan,
                    text = stringResource(id = R.string.setting_destination_folders),
                    onClick = { onNavigate(Routes.settingsDetail(SettingTypes.DESTINATIONS)) }
                )
                ActionItem(
                    enabled = appSettings.enableScan,
                    text = stringResource(id = R.string.setting_organisation_organiser_accuracy),
                    onClick = { onNavigate(Routes.settingsDetail(SettingTypes.ORGANISER_ACCURACY)) }
                )
                SelectorItem(
                    enabled = appSettings.enableScan,
                    label = stringResource(id = R.string.setting_scan_frequency),
                    options = listOf(
                        stringResource(id = R.string.scan_frequency_1d),
                        stringResource(id = R.string.scan_frequency_1w)
                    ),
                    selectedOption = appSettings.frequency,
                    onOptionSelected = { selected ->
                        viewModel.updateFrequency(selected)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

//                Text(
//                    text = stringResource(id = R.string.advanced_settings),
//                    style = MaterialTheme.typography.titleMedium,
//                    modifier = Modifier.padding(vertical = 8.dp),
//                    color = MaterialTheme.colorScheme.primary
//                )
//                ActionItem(
//                    text = stringResource(id = R.string.setting_models),
//                    onClick = { onNavigate(Routes.settingsDetail(SettingTypes.MODELS)) },
//                )
//                ActionItem(
//                    text = stringResource(id = R.string.setting_manage_models),
//                    onClick = { onNavigate(Routes.settingsDetail(SettingTypes.MANAGE_MODELS)) }
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.other_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                ActionItem(
                    text = stringResource(id = R.string.title_help),
                    onClick = { onNavigate(Routes.HELP) }
                )

                ActionItem(
                    text = stringResource(id = R.string.title_donate),
                    onClick = { onNavigate(Routes.DONATE) }
                )
                ActionItem(
                    text = stringResource(id = R.string.setting_source_code),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, sourceCodeUrl.toUri())
                        context.startActivity(intent)
                    },
                )
                ActionItem(
                    text = stringResource(id = R.string.setting_social_reddit),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, redditUrl.toUri())
                        context.startActivity(intent)
                    },
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Image(
                    painter = painterResource(id = R.drawable.smartscan_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(132.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                versionName?.let {
                    Text(
                        text = "Version $it",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = stringResource(R.string.copyright),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
