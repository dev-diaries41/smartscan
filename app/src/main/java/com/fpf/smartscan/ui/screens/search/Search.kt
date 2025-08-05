package com.fpf.smartscan.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.fpf.smartscan.R
import com.fpf.smartscan.ui.components.ProgressBar
import com.fpf.smartscan.ui.components.SelectorItem
import com.fpf.smartscan.ui.permissions.RequestPermissions
import com.fpf.smartscan.ui.screens.settings.AppSettings
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    // Index state
    val imageIndexProgress by searchViewModel.imageIndexProgress.collectAsState(initial = 0f)
    val videoIndexProgress by searchViewModel.videoIndexProgress.collectAsState(initial = 0f)
    val isIndexingImages by searchViewModel.isIndexingImages.collectAsState()
    val isIndexingVideos by searchViewModel.isIndexingVideos.collectAsState()

    // Search state
    val searchQuery by searchViewModel.query.observeAsState("")
    val isLoading by searchViewModel.isLoading.observeAsState(false)
    val error by searchViewModel.error.observeAsState(null)
    val mode by searchViewModel.mode.observeAsState(MediaType.IMAGE)
    val hasAnyIndexedImages by searchViewModel.hasAnyImages.observeAsState(null)
    val hasAnyIndexedVideos by searchViewModel.hasAnyVideos.observeAsState(null)
    val hasIndexed = when(mode) {
        MediaType.IMAGE -> hasAnyIndexedImages == true
        MediaType.VIDEO -> hasAnyIndexedVideos == true
    }
    val imageEmbeddings by searchViewModel.imageEmbeddings.observeAsState(emptyList())
    val videoEmbeddings by searchViewModel.videoEmbeddings.observeAsState(emptyList())
    val searchResults by searchViewModel.searchResults.observeAsState(emptyList())
    val resultToView by searchViewModel.resultToView.observeAsState()
    val embeddings = if (mode == MediaType.IMAGE) imageEmbeddings else videoEmbeddings
    val canSearch = hasIndexed && embeddings.isNotEmpty()
    val loadingIndexData = hasIndexed && embeddings.isEmpty()
    val showLoader = isLoading || loadingIndexData

    val appSettings by settingsViewModel.appSettings.collectAsState(AppSettings())

    val scrollState = rememberScrollState()
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    var showFirstIndexImageDialog by remember { mutableStateOf(false) }
    var showFirstIndexVideoDialog by remember { mutableStateOf(false) }

    RequestPermissions { notificationGranted, storageGranted ->
        hasNotificationPermission = notificationGranted
        hasStoragePermission = storageGranted
    }

    LaunchedEffect(hasIndexed, hasStoragePermission, mode) {
        if(hasStoragePermission && !hasIndexed && (mode == MediaType.IMAGE)){
            showFirstIndexImageDialog = true
        }else if(hasStoragePermission && !hasIndexed && (mode == MediaType.VIDEO)){
            showFirstIndexVideoDialog = true
        }
    }

    val label = if (mode == MediaType.IMAGE) "image" else "video"
    val message = stringResource(R.string.first_indexing, label)

    if ( showFirstIndexImageDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Start Indexing Images") },
            text = { Text(message) },
            dismissButton = {
                TextButton(onClick = {
                    showFirstIndexImageDialog = false
                }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFirstIndexImageDialog = false
                    searchViewModel.startIndexing()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if ( showFirstIndexVideoDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Start Indexing Videos") },
            text = { Text(message) },
            dismissButton = {
                TextButton(onClick = {
                    showFirstIndexVideoDialog = false
                }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFirstIndexVideoDialog = false
                    searchViewModel.startVideoIndexing()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            ProgressBar(
                label = "Indexing images ${"%.0f".format(imageIndexProgress * 100)}%",
                isVisible = isIndexingImages,
                progress = imageIndexProgress
            )

            ProgressBar(
                label = "Indexing videos ${"%.0f".format(videoIndexProgress * 100)}%",
                isVisible = isIndexingVideos,
                progress = videoIndexProgress
            )

            SelectorItem(
                enabled = (!isIndexingVideos && !isIndexingImages), // prevent switching modes when indexing in progress
                showLabel = false,
                label = "Search Mode",
                options = searchModeOptions.values.toList(),
                selectedOption = searchModeOptions[mode]!!,
                onOptionSelected = { selected ->
                    val newMode = searchModeOptions.entries
                        .find { it.value == selected }
                        ?.key ?: MediaType.IMAGE
                    searchViewModel.setMode(newMode)
                }
            )

            SearchBar(
                query = searchQuery,
                enabled = canSearch && hasStoragePermission,
                onSearch = searchViewModel::search,
                onQueryChange = { newQuery ->
                    searchViewModel.setQuery(newQuery)
                },
                label = when(mode){
                    MediaType.IMAGE -> "Search images..."
                    MediaType.VIDEO -> "Search videos..."
                },
                imageEmbeddings = imageEmbeddings,
                videoEmbeddings = videoEmbeddings,
                nSimilarResult = appSettings.numberSimilarResults,
                threshold = appSettings.similarityThreshold
            )

            if(searchResults.isNotEmpty()){
                TextButton(onClick = {searchViewModel.clearResults() }, modifier = Modifier.align(Alignment.End)) {
                    Text("Clear Results")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showLoader,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(durationMillis = 500)) + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                }
            }

            if(loadingIndexData && hasStoragePermission){
                Text(text = if (mode == MediaType.IMAGE) "Loading indexed images..." else "Loading indexed videos...", modifier = Modifier.padding(top=8.dp))
            }

            error?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(top=16.dp))
            }

            if(!hasStoragePermission && !isLoading){
                Text(text = stringResource(R.string.storage_permissions), color = Color.Red, modifier = Modifier.padding(top=16.dp))
            }

            SearchPlaceholderDisplay(isVisible = searchResults.isEmpty())

            Spacer(modifier = Modifier.height(16.dp))

            SearchResults(
                isVisible = !isLoading && searchResults.isNotEmpty(),
                type = mode,
                searchResults = searchResults,
                maxResults = appSettings.numberSimilarResults,
                resultToView = resultToView,
                toggleViewResult = { uri -> searchViewModel.toggleViewResult(uri) }
            )
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
            .padding(top = 64.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.ImageSearch,
                contentDescription = "Search icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(96.dp)
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
