package com.fpf.smartscan

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fpf.smartscan.constants.Routes
import com.fpf.smartscan.constants.SettingTypes
import com.fpf.smartscan.ui.components.UpdatePopUp
import com.fpf.smartscan.ui.screens.donate.DonateScreen
import com.fpf.smartscan.ui.screens.help.HelpScreen
import com.fpf.smartscan.ui.screens.search.SearchScreen
import com.fpf.smartscan.ui.screens.search.SearchViewModel
import com.fpf.smartscan.ui.screens.settings.SettingsDetailScreen
import com.fpf.smartscan.ui.screens.settings.SettingsScreen
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val typeVal = navBackStackEntry?.arguments?.getString("type")
    val mainViewModel: MainViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val isUpdatePopUpVisible by mainViewModel.isUpdatePopUpVisible.collectAsState()

    val headerTitle = when {
        currentRoute == Routes.SEARCH -> stringResource(R.string.title_search)
        currentRoute == Routes.SETTINGS -> stringResource(R.string.title_settings)
        currentRoute == Routes.DONATE -> stringResource(R.string.title_donate)
        currentRoute == Routes.HELP -> stringResource(R.string.title_help)
        currentRoute?.startsWith(Routes.SETTINGS.split("/")[0]) == true -> when (typeVal) {
            SettingTypes.THRESHOLD -> stringResource(R.string.setting_similarity_threshold)
            SettingTypes.MODELS -> stringResource(R.string.setting_models)
            SettingTypes.MANAGE_MODELS -> stringResource(R.string.setting_manage_models)
            SettingTypes.SEARCHABLE_IMG_DIRS -> stringResource(R.string.setting_searchable_image_folders)
            SettingTypes.SEARCHABLE_VID_DIRS -> stringResource(R.string.setting_searchable_video_folders)
            else -> ""
        }
        else -> ""
    }

    val showBackButton = currentRoute?.startsWith(Routes.SETTINGS.split("/")[0]) == true || currentRoute in listOf( Routes.DONATE, Routes.HELP)

    if(isUpdatePopUpVisible) {
        UpdatePopUp(
            isVisible = true,
            updates = mainViewModel.getUpdates(),
            onClose = { mainViewModel.closeUpdatePopUp() }
        )
    }else{
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = headerTitle) },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {}
                )
            },
            bottomBar = { BottomNavigationBar(navController) }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Routes.SEARCH,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Routes.SEARCH) {
                    SearchScreen(
                        searchViewModel = searchViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigate = { route: String ->
                            navController.navigate(route)
                        }
                    )
                }
                composable(
                    route = Routes.SETTINGS_DETAIL,
                    arguments = listOf(navArgument("type") { type = NavType.StringType })
                ) { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: ""
                    SettingsDetailScreen(
                        type = type,
                        viewModel = settingsViewModel,
                    )
                }
                composable(Routes.DONATE) {
                    DonateScreen()
                }

                composable(Routes.HELP) {
                    HelpScreen()
                }
            }
        }
    }
}
