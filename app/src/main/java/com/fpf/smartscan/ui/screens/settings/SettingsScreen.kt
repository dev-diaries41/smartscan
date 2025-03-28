package com.fpf.smartscan.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    Box(modifier = Modifier.fillMaxSize()) {
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
                    modifier = Modifier.padding(vertical = 8.dp)
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
                    text = stringResource(id = R.string.image_search_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                SettingsIncrementor(
                    minValue = 1,
                    maxValue = 20,
                    onDecrement = {viewModel.updateNumberSimilarImages((appSettings.numberSimilarResults - 1).toString())},
                    onIncrement = {viewModel.updateNumberSimilarImages((appSettings.numberSimilarResults + 1).toString())},
                    value = appSettings.numberSimilarResults.toString(),
                    label = stringResource(id = R.string.setting_number_similar_results),
                )


                Text(
                    text = stringResource(id = R.string.other_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                SettingsCard(
                    text = stringResource(id = R.string.setting_donate),
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
