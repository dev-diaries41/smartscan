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
import com.fpf.smartscan.ui.components.SettingsCard
import com.fpf.smartscan.ui.components.SettingsSelect
import com.fpf.smartscan.ui.components.SettingsSwitch
import com.fpf.smartscan.ui.components.SettingsIncrementor
import androidx.core.net.toUri
import com.fpf.smartscan.ui.permissions.StorageAccess
import com.fpf.smartscan.ui.permissions.getStorageAccess

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsState(AppSettings())
    val scrollState = rememberScrollState()
    val context = LocalContext.current // Access the current context
    val sourceCodeUrl = stringResource(R.string.source_code_url)
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
                                    viewModel.refreshImageIndex()
                                }else{
                                    viewModel.refreshVideoIndex()
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
                    text = stringResource(id = R.string.image_management_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                SettingsSwitch(
                    text = stringResource(id = R.string.setting_enable_scan),
                    checked = appSettings.enableScan,
                    onCheckedChange = { checked ->
                        viewModel.updateEnableScan(checked)
                    }
                )
                SettingsCard(
                    enabled = appSettings.enableScan,
                    text = stringResource(id = R.string.setting_target_folders),
                    onClick = { onNavigate("settingsDetail/targets") }
                )
                SettingsCard(
                    enabled = appSettings.enableScan,
                    text = stringResource(id = R.string.setting_destination_folders),
                    onClick = { onNavigate("settingsDetail/destinations") }
                )
                SettingsSelect(
                    enabled = appSettings.enableScan,
                    label = stringResource(id = R.string.setting_scan_frequency),
                    options = listOf(
                        stringResource(id = R.string.scan_frequency_1d),
                        stringResource(id = R.string.scan_frequency_1w)
                    ),
                    selectedOption = appSettings.frequency,
                    onOptionSelected = { selected ->
                        viewModel.updateFrequency(selected)
                    }
                )
                Text(
                    text = stringResource(id = R.string.media_search_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                SettingsIncrementor(
                    minValue = 1,
                    maxValue = 20,
                    onDecrement = {viewModel.updateNumberSimilarImages((appSettings.numberSimilarResults - 1).toString())},
                    onIncrement = {viewModel.updateNumberSimilarImages((appSettings.numberSimilarResults + 1).toString())},
                    value = appSettings.numberSimilarResults.toString(),
                    label = stringResource(id = R.string.setting_number_similar_results),
                )

                SettingsCard(
                    text = stringResource(id = R.string.setting_similarity_threshold),
                    onClick = { onNavigate("settingsDetail/threshold") }
                )


                SettingsSelect(
                    label = stringResource(id = R.string.setting_index_frequency),
                    options = listOf(
                        stringResource(id = R.string.scan_frequency_1d),
                        stringResource(id = R.string.scan_frequency_1w)
                    ),
                    selectedOption = appSettings.indexFrequency,
                    onOptionSelected = { selected ->
                        viewModel.updateIndexFrequency(selected)
                    },
//                    description = stringResource(id = R.string.setting_index_frequency_description)
                )

                SettingsCard(
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

                SettingsCard(
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


                Text(
                    text = stringResource(id = R.string.other_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                SettingsCard(
                    text = stringResource(id = R.string.title_help),
                    onClick = { onNavigate("help") }
                )

                SettingsCard(
                    text = stringResource(id = R.string.title_donate),
                    onClick = { onNavigate("donate") }
                )
                SettingsCard(
                    text = stringResource(id = R.string.setting_source_code),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, sourceCodeUrl.toUri())
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
