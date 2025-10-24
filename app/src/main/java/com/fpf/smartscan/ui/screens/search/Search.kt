package com.fpf.smartscan.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.data.ProcessorStatus
import com.fpf.smartscan.data.QueryType
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.services.startIndexing
import com.fpf.smartscan.ui.components.LoadingIndicator
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.ProgressBar
import com.fpf.smartscan.ui.components.SelectorIconItem
import com.fpf.smartscan.ui.components.search.ImageSearcher
import com.fpf.smartscan.ui.components.search.SearchBar
import com.fpf.smartscan.ui.components.search.SearchResults
import com.fpf.smartscan.ui.permissions.RequestPermissions
import com.fpf.smartscan.ui.screens.search.SearchViewModel.Companion.RESULTS_BATCH_SIZE
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val appSettings by settingsViewModel.appSettings.collectAsState()

    // Index state
    val imageIndexProgress by searchViewModel.imageIndexProgress.collectAsState(initial = 0f)
    val videoIndexProgress by searchViewModel.videoIndexProgress.collectAsState(initial = 0f)
    val imageIndexStatus by searchViewModel.imageIndexStatus.collectAsState()
    val videoIndexStatus by searchViewModel.videoIndexStatus.collectAsState()
    val isImageIndexAlertVisible by searchViewModel.isImageIndexAlertVisible.collectAsState(false)
    val isVideoIndexAlertVisible by searchViewModel.isVideoIndexAlertVisible.collectAsState(false)

    // Search state
    val isLoading by searchViewModel.isLoading.collectAsState(false)
    val error by searchViewModel.error.collectAsState(null)
    val mediaType by searchViewModel.mediaType.collectAsState(MediaType.IMAGE)
    val hasIndexed by searchViewModel.hasIndexed.collectAsState(null)
    val searchResults by searchViewModel.searchResults.collectAsState(emptyList())
    val resultToView by searchViewModel.resultToView.collectAsState()
    val canSearch by searchViewModel.canSearch.collectAsState(false)
    val queryType by searchViewModel.queryType.collectAsState()
    val searchImageUri by searchViewModel.searchImageUri.collectAsState()
    val totalResults by searchViewModel.totalResults.collectAsState()

    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }

    RequestPermissions { notificationGranted, storageGranted ->
        hasNotificationPermission = notificationGranted
        hasStoragePermission = storageGranted
    }

    LaunchedEffect(hasIndexed, hasStoragePermission, mediaType) {
        if(hasStoragePermission && hasIndexed == false && (mediaType == MediaType.IMAGE)){
            searchViewModel.toggleAlert(MediaType.IMAGE)
        }else if(hasStoragePermission && hasIndexed == false && (mediaType == MediaType.VIDEO)){
            searchViewModel.toggleAlert(MediaType.VIDEO)
        }
    }

    LaunchedEffect(imageIndexStatus) {
        if (imageIndexStatus == ProcessorStatus.COMPLETE) {
            searchViewModel.refreshIndex(MediaType.IMAGE)
        }
    }

    LaunchedEffect(videoIndexStatus) {
        if (videoIndexStatus == ProcessorStatus.COMPLETE) {
            searchViewModel.refreshIndex(MediaType.VIDEO)
        }
    }

    val label = if (mediaType == MediaType.IMAGE) "image" else "video"
    val message = stringResource(R.string.first_indexing, label)

    if ( isImageIndexAlertVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Start Indexing Images") },
            text = { Text(message) },
            dismissButton = {
                TextButton(onClick = {
                    searchViewModel.toggleAlert(MediaType.IMAGE)
                }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    searchViewModel.toggleAlert(MediaType.IMAGE)
                    startIndexing(context, MediaIndexForegroundService.TYPE_IMAGE)
                }) {
                    Text("OK")
                }
            }
        )
    }

    if ( isVideoIndexAlertVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Start Indexing Videos") },
            text = { Text(message) },
            dismissButton = {
                TextButton(onClick = {
                    searchViewModel.toggleAlert(MediaType.VIDEO)
                }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    searchViewModel.toggleAlert(MediaType.VIDEO)
                    startIndexing(context, MediaIndexForegroundService.TYPE_VIDEO)
                }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            ProgressBar(
                label = "Indexing images ${"%.0f".format(imageIndexProgress * 100)}%",
                isVisible = imageIndexStatus == ProcessorStatus.ACTIVE,
                progress = imageIndexProgress
            )

            ProgressBar(
                label = "Indexing videos ${"%.0f".format(videoIndexProgress * 100)}%",
                isVisible = videoIndexStatus == ProcessorStatus.ACTIVE,
                progress = videoIndexProgress
            )

            if(queryType == QueryType.IMAGE){
                ImageSearcher(
                    uri = searchImageUri,
                    threshold = appSettings.similarityThreshold,
                    mediaType = mediaType,
                    searchEnabled = canSearch && searchImageUri != null,
                    mediaTypeSelectorEnabled = (videoIndexStatus != ProcessorStatus.ACTIVE && imageIndexStatus != ProcessorStatus.ACTIVE), // prevent switching modes when indexing in progress
                    onSearch = searchViewModel::imageSearch,
                    onMediaTypeChange = searchViewModel::setMediaType,
                    onRemoveImage = {
                        searchViewModel.updateSearchImageUri(null)
                        searchViewModel.updateQueryType(QueryType.TEXT)
                    }
                )
            }else{
                SearchBar(
                    enabled = canSearch && hasStoragePermission && !isLoading,
                    onSearch = searchViewModel::textSearch,
                    onImageSelected = {
                        searchViewModel.updateSearchImageUri(it)
                        searchViewModel.updateQueryType(QueryType.IMAGE)
                                      },
                    onImagePasted = {
                        searchViewModel.updateSearchImageUri(it)
                        searchViewModel.updateQueryType(QueryType.IMAGE)
                    },
                    label = when (mediaType) {
                        MediaType.IMAGE -> "Search images..."
                        MediaType.VIDEO -> "Search videos..."
                    },
                    threshold = appSettings.similarityThreshold,
                    trailingIcon = {
                        SelectorIconItem(
                            enabled = (videoIndexStatus != ProcessorStatus.ACTIVE && imageIndexStatus != ProcessorStatus.ACTIVE), // prevent switching modes when indexing in progress
                            label = "Media type",
                            options = mediaTypeOptions.values.toList(),
                            selectedOption = mediaTypeOptions[mediaType]!!,
                            onOptionSelected = { selected ->
                                val newMode = mediaTypeOptions.entries
                                    .find { it.value == selected }
                                    ?.key ?: MediaType.IMAGE
                                searchViewModel.setMediaType(newMode)
                            }
                        )
                    }
                )
            }


            if(searchResults.isNotEmpty()){
                TextButton(onClick = {searchViewModel.clearResults() },  modifier = Modifier.align(Alignment.End)) {
                    Text("Clear results")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LoadingIndicator(isVisible = isLoading, size = 48.dp, strokeWidth = 4.dp, modifier = Modifier.fillMaxWidth())

            if(!canSearch && hasIndexed == true && isLoading && hasStoragePermission){
                Text(text = if (mediaType == MediaType.IMAGE) "Loading indexed images..." else "Loading indexed videos...", modifier = Modifier.padding(top=8.dp))
            }

            error?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(top=16.dp))
            }

            if(!hasStoragePermission && !isLoading){
                Text(text = stringResource(R.string.storage_permissions), color = Color.Red, modifier = Modifier.padding(top=16.dp))
            }

            SearchPlaceholderDisplay(isVisible = searchResults.isEmpty())

            SearchResults(
                isVisible = !isLoading && searchResults.isNotEmpty(),
                type = mediaType,
                searchResults = searchResults,
                toggleViewResult = searchViewModel::toggleViewResult,
                updateSearchImage = {
                    searchViewModel.updateSearchImageUri(it)
                    searchViewModel.updateQueryType(QueryType.IMAGE)
                                    },
                onLoadMore = searchViewModel::onLoadMore,
                totalResults=totalResults,
                loadMoreBuffer = (RESULTS_BATCH_SIZE * 0.2).toInt()
            )
        }
        resultToView?.let { uri ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
            ) {
                MediaViewer(
                    uri = uri,
                    type = mediaType,
                    onClose = { searchViewModel.toggleViewResult(null) },
                    onUpdateSearchImage = {
                        searchViewModel.updateSearchImageUri(uri)
                        searchViewModel.toggleViewResult(null)
                        searchViewModel.updateQueryType(QueryType.IMAGE)
                    }
                )
            }
        }
    }

}


@Composable
fun SearchPlaceholderDisplay(isVisible: Boolean) {
    if(!isVisible) return

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.ImageSearch,
                contentDescription = "Search icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
            Text(
                textAlign = TextAlign.Center,
                text = "Find what you're looking for",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

