package com.fpf.smartscan

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.res.stringResource
import com.fpf.smartscan.ui.screens.donate.DonateScreen
import com.fpf.smartscan.ui.screens.scanhistory.ScanHistoryViewModel
import com.fpf.smartscan.ui.screens.scanhistory.ScanHistoryScreen
import com.fpf.smartscan.ui.screens.search.SearchScreen
import com.fpf.smartscan.ui.screens.settings.SettingsDetailScreen
import com.fpf.smartscan.ui.screens.settings.SettingsScreen
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import com.fpf.smartscan.ui.screens.test.TestScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val typeVal = navBackStackEntry?.arguments?.getString("type")
    val settingsViewModel: SettingsViewModel = viewModel()

    val headerTitle = when {
        currentRoute == "search" -> stringResource(R.string.title_search)
        currentRoute == "scanhistory" -> stringResource(R.string.title_scan_history)
        currentRoute == "settings" -> stringResource(R.string.title_settings)
        currentRoute == "donate" -> stringResource(R.string.setting_donate)
        currentRoute == "test" -> stringResource(R.string.setting_test_model)
        currentRoute?.startsWith("settingsDetail") == true -> when (typeVal) {
            "targets" -> stringResource(R.string.setting_target_folders)
            "threshold" -> stringResource(R.string.setting_similarity_threshold)
            else -> stringResource(R.string.setting_destination_folders)
        }
        else -> ""
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = headerTitle) },
                navigationIcon = {
                    if (currentRoute?.startsWith("settingsDetail") == true || currentRoute?.startsWith("test") == true || currentRoute == "donate" || currentRoute == "scanhistory") {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute != "scanhistory") {
                        IconButton(onClick = { navController.navigate("scanhistory") }) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "Scan History"
                            )
                        }
                    }
                    if (currentRoute != "test") {
                        IconButton(onClick = { navController.navigate("test") }) {
                            Icon(
                                imageVector = Icons.Filled.Science,
                                contentDescription = "Test Model"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "search",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("search") {
                SearchScreen()
            }
            composable("scanhistory") {
                val scanHistoryViewModel: ScanHistoryViewModel = viewModel()
                ScanHistoryScreen(
                    viewModel = scanHistoryViewModel,
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigate = { route: String ->
                        navController.navigate(route)
                    }
                )
            }
            composable(
                route = "settingsDetail/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: ""
                SettingsDetailScreen(
                    type = type,
                    viewModel = settingsViewModel,
                )
            }
            composable("test"){
                TestScreen(
                    settingsViewModel = settingsViewModel
                )
            }
            composable("donate"){
                DonateScreen()
            }
        }
    }
}