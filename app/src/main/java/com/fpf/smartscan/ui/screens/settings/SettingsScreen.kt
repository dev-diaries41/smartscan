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
import com.fpf.smartscan.ui.components.ActionItem
import com.fpf.smartscan.ui.components.SelectorItem
import androidx.core.net.toUri
import com.fpf.smartscan.constants.Routes
import com.fpf.smartscan.constants.SettingTypes
import com.fpf.smartscan.constants.colorSchemeDisplayNames
import com.fpf.smartscan.constants.themeModeDisplayNames
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val sourceCodeUrl = stringResource(R.string.source_code_url)
    val redditUrl = stringResource(R.string.reddit_url)
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
                ActionItem(
                    text = stringResource(id = R.string.setting_similarity_threshold),
                    onClick = { onNavigate(Routes.settingsDetail(SettingTypes.THRESHOLD)) }
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
