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
import com.fpf.smartscan.constants.Routes
import com.fpf.smartscan.constants.SettingTypes
import com.fpf.smartscan.ui.components.UpdatePopUp
import com.fpf.smartscan.ui.screens.donate.DonateScreen
import com.fpf.smartscan.ui.screens.help.HelpScreen
import com.fpf.smartscan.ui.screens.scanhistory.ScanHistoryViewModel
import com.fpf.smartscan.ui.screens.scanhistory.ScanHistoryScreen
import com.fpf.smartscan.ui.screens.search.SearchScreen
import com.fpf.smartscan.ui.screens.search.SearchViewModel
import com.fpf.smartscan.ui.screens.settings.SettingsDetailScreen
import com.fpf.smartscan.ui.screens.settings.SettingsScreen
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import com.fpf.smartscan.ui.screens.tags.TagEditScreen
import com.fpf.smartscan.ui.screens.tags.TagManagerScreen
import com.fpf.smartscan.ui.screens.retagging.RetaggingScreen
import com.fpf.smartscan.ui.screens.test.TestScreen
import com.fpf.smartscan.workers.ClassificationViewModel
import java.util.UUID

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
    val classificationViewModel: ClassificationViewModel = viewModel()
    val isOrganiseActive by classificationViewModel.organisationActive.collectAsState(false)
    val isUpdatePopUpVisible by mainViewModel.isUpdatePopUpVisible.collectAsState()

    val headerTitle = when {
        currentRoute == Routes.SEARCH -> stringResource(R.string.title_search)
        currentRoute == Routes.SCAN_HISTORY -> stringResource(R.string.title_scan_history)
        currentRoute == Routes.SETTINGS -> stringResource(R.string.title_settings)
        currentRoute == Routes.DONATE -> stringResource(R.string.title_donate)
        currentRoute == Routes.HELP -> stringResource(R.string.title_help)
        currentRoute == Routes.TEST -> stringResource(R.string.title_test_organisation)
        currentRoute == Routes.TAG_MANAGER -> "Správa tagů"
        currentRoute == Routes.TAG_ADD -> "Přidat tag"
        currentRoute?.startsWith("tag_edit") == true -> "Upravit tag"
        currentRoute?.startsWith(Routes.SETTINGS.split("/")[0]) == true -> when (typeVal) {
            SettingTypes.TARGETS -> stringResource(R.string.setting_target_folders)
            SettingTypes.THRESHOLD -> stringResource(R.string.setting_similarity_threshold)
            SettingTypes.DESTINATIONS -> stringResource(R.string.setting_destination_folders)
            SettingTypes.ORGANISER_ACCURACY -> stringResource(R.string.setting_organisation_organiser_accuracy)
            SettingTypes.MODELS -> stringResource(R.string.setting_models)
            SettingTypes.MANAGE_MODELS -> stringResource(R.string.setting_manage_models)
            SettingTypes.SEARCHABLE_IMG_DIRS -> stringResource(R.string.setting_searchable_image_folders)
            SettingTypes.SEARCHABLE_VID_DIRS -> stringResource(R.string.setting_searchable_video_folders)
            else -> ""
        }
        else -> ""
    }

    val showBackButton = currentRoute?.startsWith(Routes.SETTINGS.split("/")[0]) == true ||
            currentRoute in listOf(Routes.TEST, Routes.DONATE, Routes.SCAN_HISTORY, Routes.HELP, Routes.TAG_MANAGER, Routes.TAG_ADD, Routes.RETAGGING) ||
            currentRoute?.startsWith("tag_edit") == true

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
                    actions = {
                        if (currentRoute != Routes.SCAN_HISTORY) {
                            IconButton(onClick = { navController.navigate(Routes.SCAN_HISTORY) }) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = "Scan History"
                                )
                            }
                        }
                        if (currentRoute != Routes.TEST) {
                            IconButton(onClick = { navController.navigate(Routes.TEST) }) {
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
                                    animation = tween(
                                        durationMillis = 10000,
                                        easing = LinearEasing
                                    ),
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
                startDestination = Routes.SEARCH,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Routes.SEARCH) {
                    SearchScreen(
                        searchViewModel = searchViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
                composable(Routes.SCAN_HISTORY) {
                    val scanHistoryViewModel: ScanHistoryViewModel = viewModel()
                    ScanHistoryScreen(
                        viewModel = scanHistoryViewModel,
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
                composable(Routes.TEST) {
                    TestScreen(
                        settingsViewModel = settingsViewModel
                    )
                }
                composable(Routes.DONATE) {
                    DonateScreen()
                }

                composable(Routes.HELP) {
                    HelpScreen()
                }

                // Tag management routes
                composable(Routes.TAG_MANAGER) {
                    TagManagerScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { tagName ->
                            if (tagName == null) {
                                navController.navigate(Routes.TAG_ADD)
                            } else {
                                navController.navigate(Routes.tagEdit(tagName))
                            }
                        },
                        onNavigateToRetagging = { workId ->
                            navController.navigate("${Routes.RETAGGING}/${workId}")
                        }
                    )
                }

                composable(Routes.TAG_ADD) {
                    TagEditScreen(
                        tagName = null,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.TAG_EDIT,
                    arguments = listOf(navArgument("tagName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tagName = backStackEntry.arguments?.getString("tagName")
                    TagEditScreen(
                        tagName = tagName,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Retagging screen
                composable(
                    route = "${Routes.RETAGGING}/{workId}",
                    arguments = listOf(navArgument("workId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val workIdString = backStackEntry.arguments?.getString("workId") ?: ""
                    val workId = try {
                        UUID.fromString(workIdString)
                    } catch (e: Exception) {
                        null
                    }

                    if (workId != null) {
                        RetaggingScreen(
                            workId = workId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    } else {
                        // Fallback - invalid work ID
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
