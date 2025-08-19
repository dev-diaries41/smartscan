package com.fpf.smartscan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fpf.smartscan.ui.screens.donate.DonateScreen
import com.fpf.smartscan.ui.screens.help.HelpScreen
import com.fpf.smartscan.ui.screens.scanhistory.ScanHistoryViewModel
import com.fpf.smartscan.ui.screens.scanhistory.ScanHistoryScreen
import com.fpf.smartscan.ui.screens.search.SearchScreen
import com.fpf.smartscan.ui.screens.search.SearchViewModel
import com.fpf.smartscan.ui.screens.settings.SettingsDetailScreen
import com.fpf.smartscan.ui.screens.settings.SettingsScreen
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import com.fpf.smartscan.ui.screens.test.TestScreen
import com.fpf.smartscan.workers.ClassificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val typeVal = navBackStackEntry?.arguments?.getString("type")
    val settingsViewModel: SettingsViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val classificationViewModel: ClassificationViewModel = viewModel()
    val isOrganiseActive by classificationViewModel.organisationActive.collectAsState(false)

    val headerTitle = when {
        currentRoute == "search" -> stringResource(R.string.title_search)
        currentRoute == "scanhistory" -> stringResource(R.string.title_scan_history)
        currentRoute == "settings" -> stringResource(R.string.title_settings)
        currentRoute == "donate" -> stringResource(R.string.title_donate)
        currentRoute == "help" -> stringResource(R.string.title_help)
        currentRoute == "test" -> stringResource(R.string.title_test_organisation)
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
                    if (currentRoute?.startsWith("settingsDetail") == true || currentRoute?.startsWith("test") == true || currentRoute == "donate" || currentRoute == "scanhistory" || currentRoute == "help") {
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

                    if (isOrganiseActive) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 10000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            modifier = Modifier.rotate(rotation),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "Organisation is active"
                        )
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
                SearchScreen(
                    searchViewModel=searchViewModel,
                    settingsViewModel=settingsViewModel
                )
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

            composable("help"){
                HelpScreen()
            }
        }
    }
}