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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.fpf.smartscan.R
import com.fpf.smartscan.ui.permissions.RequestPermissions
import com.fpf.smartscan.ui.screens.settings.AppSettings
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import com.fpf.smartscan.workers.scheduleImageIndexWorker

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val searchQuery by searchViewModel.query.observeAsState("")
    val isLoading by searchViewModel.isLoading.observeAsState(false)
    val error by searchViewModel.error.observeAsState(null)
    val imageEmbeddings by searchViewModel.imageEmbeddings.observeAsState(emptyList())
    val searchResults by searchViewModel.searchResults.observeAsState(emptyList())
    val isFirstIndex by searchViewModel.isFirstIndex.observeAsState(false)
    val appSettings by settingsViewModel.appSettings.collectAsState(AppSettings())
    val scrollState = rememberScrollState()

    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    var showFirstIndexDialog by remember { mutableStateOf(isFirstIndex) }
    var hasScheduledIndexing by remember { mutableStateOf(false) }

    RequestPermissions { notificationGranted, storageGranted ->
        hasNotificationPermission = notificationGranted
        hasStoragePermission = storageGranted
    }

    LaunchedEffect(isFirstIndex, hasStoragePermission) {
        showFirstIndexDialog = isFirstIndex && hasStoragePermission
        if(hasStoragePermission && isFirstIndex && !hasScheduledIndexing){
            scheduleImageIndexWorker(context, "1 Week")
            hasScheduledIndexing = true
        }
    }

    if (isFirstIndex && showFirstIndexDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Indexing Images") },
            text = {
                Text(stringResource(R.string.first_index))
            },
            confirmButton = {
                TextButton(onClick = { showFirstIndexDialog = false }) {
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


            OutlinedTextField(
                enabled = !isLoading && hasStoragePermission,
                value = searchQuery,
                onValueChange = { newQuery ->
                    searchViewModel.setQuery(newQuery)
                },
                label = { Text("Search images...", color = Color.White) },
                textStyle = TextStyle(color = Color.White),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    IconButton(
                        enabled = !isLoading && hasStoragePermission,
                        onClick = {
                            searchViewModel.searchImages(appSettings.numberSimilarResults, imageEmbeddings)
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
                visible = isLoading,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(durationMillis = 500)) + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White,
                        strokeWidth = 4.dp
                    )
                }
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
                SearchResults(
                    initialMainResult = mainResult,
                    similarResults = similarResults
                )
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
