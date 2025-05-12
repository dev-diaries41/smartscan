package com.fpf.smartscan.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.fpf.smartscan.R
import com.fpf.smartscan.ui.components.SettingsSelect
import com.fpf.smartscan.ui.permissions.RequestPermissions
import com.fpf.smartscan.ui.screens.settings.AppSettings
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val imageIndexProgress by searchViewModel.imageIndexProgress.collectAsState(initial = 0f)
    val videoIndexProgress by searchViewModel.videoIndexProgress.collectAsState(initial = 0f)
    val searchQuery by searchViewModel.query.observeAsState("")
    val isLoading by searchViewModel.isLoading.observeAsState(false)
    val error by searchViewModel.error.observeAsState(null)
    val hasAnyIndexedImages by searchViewModel.hasAnyImages.observeAsState(null)
    val hasAnyIndexedVideos by searchViewModel.hasAnyVideos.observeAsState(null)
    val imageEmbeddings by searchViewModel.imageEmbeddings.observeAsState(emptyList())
    val videoEmbeddings by searchViewModel.videoEmbeddings.observeAsState(emptyList())
    val searchResults by searchViewModel.searchResults.observeAsState(emptyList())
    val mode by searchViewModel.mode.observeAsState(SearchMode.IMAGE)
    val isFirstIndex by searchViewModel.isFirstIndex.observeAsState(false)
    val isFirstVideoIndex by searchViewModel.isFirstVideoIndex.observeAsState(false)
    val appSettings by settingsViewModel.appSettings.collectAsState(AppSettings())
    val scrollState = rememberScrollState()

    // Search state
    val hasIndexed = when(mode) {
        SearchMode.IMAGE -> hasAnyIndexedImages == true
        SearchMode.VIDEO -> hasAnyIndexedVideos == true
    }
    val embeddings = if (mode == SearchMode.IMAGE) imageEmbeddings else videoEmbeddings
    val canSearch = hasIndexed && embeddings.isNotEmpty()
    val loadingIndexData = hasIndexed && embeddings.isEmpty()
    val showLoader = isLoading || loadingIndexData

    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    var showFirstIndexImageDialog by remember { mutableStateOf(false) }
    var showFirstIndexVideoDialog by remember { mutableStateOf(false) }

    RequestPermissions { notificationGranted, storageGranted ->
        hasNotificationPermission = notificationGranted
        hasStoragePermission = storageGranted
    }

    LaunchedEffect(isFirstIndex, hasStoragePermission, isFirstVideoIndex, mode) {
        if(hasStoragePermission && isFirstIndex && (mode == SearchMode.IMAGE)){
            showFirstIndexImageDialog = true
            searchViewModel.scheduleIndexing(appSettings.indexFrequency)
        }else if(hasStoragePermission && isFirstVideoIndex && (mode == SearchMode.VIDEO)){
            showFirstIndexVideoDialog = true
            searchViewModel.scheduleVideoIndexing(appSettings.indexFrequency)
        }
    }

    val label = if (mode == SearchMode.IMAGE) "image" else "video"
    val message = stringResource(R.string.first_indexing, label)

    if ( showFirstIndexImageDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Indexing Images") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { showFirstIndexImageDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if ( showFirstIndexVideoDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Indexing Videos") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { showFirstIndexVideoDialog = false }) {
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
            if (imageIndexProgress > 0f && imageIndexProgress < 1f) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Indexing ${"%.0f".format(imageIndexProgress * 100)}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearProgressIndicator(
                        progress = { imageIndexProgress},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }

            if (videoIndexProgress > 0f && videoIndexProgress <1f) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Indexing video ${"%.0f".format(videoIndexProgress * 100)}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearProgressIndicator(
                        progress = { videoIndexProgress},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }

            SettingsSelect(
                showLabel = false,
                label = "Search Mode",
                options = searchModeOptions.values.toList(),
                selectedOption = searchModeOptions[mode]!!,
                onOptionSelected = { selected ->
                    val newMode = searchModeOptions.entries
                        .find { it.value == selected }
                        ?.key ?: SearchMode.IMAGE
                    searchViewModel.setMode(newMode)
                }
            )

            OutlinedTextField(
                enabled = canSearch && hasStoragePermission,
                value = searchQuery,
                onValueChange = { newQuery ->
                    searchViewModel.setQuery(newQuery)
                },
                label = { Text(text=when(mode){
                    SearchMode.IMAGE -> "Search images..."
                    SearchMode.VIDEO -> "Search videos..."
                }) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardActions = KeyboardActions (
                    onSearch = {
                        when (mode) {
                            SearchMode.IMAGE ->
                                searchViewModel.searchImages(appSettings.numberSimilarResults, imageEmbeddings, appSettings.similarityThreshold
                                )

                            SearchMode.VIDEO ->
                                searchViewModel.searchVideos(appSettings.numberSimilarResults, videoEmbeddings, appSettings.similarityThreshold
                                )
                        }
                    }
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                trailingIcon = {
                    IconButton(
                        enabled = canSearch && hasStoragePermission && searchQuery.isNotEmpty(),
                        onClick = {
                            when (mode) {
                                SearchMode.IMAGE ->
                                    searchViewModel.searchImages(appSettings.numberSimilarResults, imageEmbeddings, appSettings.similarityThreshold
                                    )

                                SearchMode.VIDEO ->
                                    searchViewModel.searchVideos(appSettings.numberSimilarResults, videoEmbeddings, appSettings.similarityThreshold
                                    )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFFC5C7D)
                        )
                    }
                }
            )

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
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = when(mode){
                    SearchMode.IMAGE -> "Loading indexed images..."
                    SearchMode.VIDEO -> "Loading indexed videos..."
                })
            }

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = Color.Red)
            }

            if(!hasStoragePermission && !isLoading){
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.storage_permissions), color = Color.Red)
            }

            if(searchResults.isEmpty()){
                Spacer(modifier = Modifier.height(48.dp))
                SearchPlaceholderDisplay()
            }

            if (!isLoading && searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                val mainResult = searchResults.first()
                val similarResults = if (searchResults.size > 1) searchResults.drop(1).take(appSettings.numberSimilarResults) else emptyList()

                when(mode){
                    SearchMode.IMAGE -> SearchResults(
                        initialMainResult = mainResult,
                        similarResults = similarResults,
                        onClear = { searchViewModel.clearResults() }
                    )
                    SearchMode.VIDEO -> VideoSearchResults(
                        initialMainResult = mainResult,
                        similarResults = similarResults,
                        onClear = { searchViewModel.clearResults() }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchPlaceholderDisplay() {
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
