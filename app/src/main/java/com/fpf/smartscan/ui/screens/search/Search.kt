package com.fpf.smartscan.ui.screens.search

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.launch
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.constants.queryOptions
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.data.ProcessorStatus
import com.fpf.smartscan.data.QueryType
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.services.startIndexing
import com.fpf.smartscan.ui.components.LoadingIndicator
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.ProgressBar
import com.fpf.smartscan.ui.components.SelectorIconItem
import com.fpf.smartscan.ui.components.SelectorItem
import com.fpf.smartscan.ui.components.search.ImageSearcher
import com.fpf.smartscan.ui.components.search.SearchBar
import com.fpf.smartscan.ui.components.search.SearchResults
import com.fpf.smartscan.ui.components.search.SelectionActionBar
import com.fpf.smartscan.ui.components.search.TagFilterChips
import com.fpf.smartscan.ui.components.search.DateRangeFilterDialog
import com.fpf.smartscan.ui.components.search.getDateRangeDescription
import com.fpf.smartscan.ui.components.search.FewShotSelector
import com.fpf.smartscan.ui.components.search.FilterSection
import com.fpf.smartscan.ui.components.search.UnifiedSearchBar
import com.fpf.smartscan.ui.components.search.ActiveFiltersChips
import com.fpf.smartscan.ui.components.media.SwipeableMediaViewer
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
    val searchQuery by searchViewModel.query.collectAsState("")
    val translatedQuery by searchViewModel.translatedQuery.collectAsState(null)
    val isLoading by searchViewModel.isLoading.collectAsState(false)
    val error by searchViewModel.error.collectAsState(null)
    val mediaType by searchViewModel.mediaType.collectAsState(MediaType.IMAGE)
    val hasIndexed by searchViewModel.hasIndexed.collectAsState(null)
    val searchResults by searchViewModel.searchResults.collectAsState(emptyList())
    val resultToView by searchViewModel.resultToView.collectAsState()
    val viewerIndex by searchViewModel.viewerIndex.collectAsState()
    val canSearch by searchViewModel.canSearch.collectAsState(false)
    val queryType by searchViewModel.queryType.collectAsState()
    val searchImageUri by searchViewModel.searchImageUri.collectAsState()
    val totalResults by searchViewModel.totalResults.collectAsState()

    // Crop state
    val showCropDialog by searchViewModel.showCropDialog.collectAsState()
    val croppedBitmap by searchViewModel.croppedBitmap.collectAsState()

    // Selection state
    val isSelectionMode by searchViewModel.isSelectionMode.collectAsState()
    val selectedUris by searchViewModel.selectedUris.collectAsState()

    // Tag filtering state
    val availableTagsWithCounts by searchViewModel.availableTagsWithCounts.collectAsState()
    val selectedTagFilters by searchViewModel.selectedTagFilters.collectAsState()

    // Date range filtering state
    val dateRangeStart by searchViewModel.dateRangeStart.collectAsState()
    val dateRangeEnd by searchViewModel.dateRangeEnd.collectAsState()

    // Few-Shot Learning state
    val availableFewShotPrototypes by searchViewModel.availableFewShotPrototypes.collectAsState()
    val selectedFewShotPrototype by searchViewModel.selectedFewShotPrototype.collectAsState()

    var showDateRangeDialog by remember { mutableStateOf(false) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMoveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteSuccessMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Načtení tagů a few-shot prototypes při startu
    LaunchedEffect(Unit) {
        searchViewModel.loadAvailableTagsWithCounts()
        searchViewModel.loadAvailableFewShotPrototypes()
    }

    // Directory picker pro přesun souborů
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { destinationUri ->
            context.contentResolver.takePersistableUriPermission(
                destinationUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scope.launch {
                val (success, failed) = searchViewModel.moveSelectedFiles(destinationUri)
                if (success > 0 || failed > 0) {
                    showMoveSuccessMessage = "Přesunuto: $success, Selhalo: $failed"
                }
            }
        }
    }

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
            title = { Text(stringResource(R.string.dialog_start_indexing_images_title)) },
            text = { Text(message) },
            dismissButton = {
                TextButton(onClick = {
                    searchViewModel.toggleAlert(MediaType.IMAGE)
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    searchViewModel.toggleAlert(MediaType.IMAGE)
                    startIndexing(context, MediaIndexForegroundService.TYPE_IMAGE)
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }

    if ( isVideoIndexAlertVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.dialog_start_indexing_videos_title)) },
            text = { Text(message) },
            dismissButton = {
                TextButton(onClick = {
                    searchViewModel.toggleAlert(MediaType.VIDEO)
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    searchViewModel.toggleAlert(MediaType.VIDEO)
                    startIndexing(context, MediaIndexForegroundService.TYPE_VIDEO)
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }

    // Dialog pro potvrzení smazání
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_files_title)) },
            text = { Text(stringResource(R.string.delete_files_message, selectedUris.size)) },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    scope.launch {
                        val (success, failed) = searchViewModel.deleteSelectedFiles()
                        if (success > 0 || failed > 0) {
                            showDeleteSuccessMessage = "Smazáno: $success, Selhalo: $failed"
                        }
                    }
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Snackbar pro úspěšný přesun
    showMoveSuccessMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            showMoveSuccessMessage = null
        }
    }

    // Snackbar pro úspěšné smazání
    showDeleteSuccessMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            showDeleteSuccessMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Selection action bar
            if (isSelectionMode) {
                SelectionActionBar(
                    selectedCount = selectedUris.size,
                    onClose = { searchViewModel.toggleSelectionMode() },
                    onSelectAll = { searchViewModel.selectAllUris() },
                    onMove = { directoryPickerLauncher.launch(null) },
                    onDelete = { showDeleteConfirmDialog = true },
                    onShare = { searchViewModel.shareSelectedFiles() }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
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

            // Unified Search Bar podle UI mockupu
            UnifiedSearchBar(
                query = searchQuery,
                onQueryChange = { newQuery -> searchViewModel.setQuery(newQuery) },
                onClearQuery = { searchViewModel.setQuery("") },
                queryType = queryType,
                onQueryTypeToggle = {
                    val newType = if (queryType == QueryType.TEXT) QueryType.IMAGE else QueryType.TEXT
                    searchViewModel.updateQueryType(newType)
                },
                mediaType = mediaType,
                onSearch = {
                    when (queryType) {
                        QueryType.TEXT -> searchViewModel.textSearch(appSettings.similarityThreshold)
                        QueryType.IMAGE -> searchViewModel.imageSearch(appSettings.similarityThreshold)
                    }
                },
                enabled = canSearch && hasStoragePermission && !isLoading,
                translatedQuery = translatedQuery,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Active Filters Chips (pouze pro IMAGE mode)
            if (mediaType == MediaType.IMAGE) {
                ActiveFiltersChips(
                    selectedTags = selectedTagFilters,
                    onTagRemove = { tagName -> searchViewModel.toggleTagFilter(tagName) },
                    selectedFewShotPrototype = selectedFewShotPrototype,
                    onFewShotRemove = { searchViewModel.selectFewShotPrototype(null) },
                    dateRangeStart = dateRangeStart,
                    dateRangeEnd = dateRangeEnd,
                    onDateRangeRemove = { searchViewModel.clearDateRange() }
                )
            }

            // Media Type Toggle - dočasně ponecháno, bude přesunuto do NavigationBar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                MediaTypeToggle(
                    currentMediaType = mediaType,
                    onMediaTypeChange = { newType -> searchViewModel.setMediaType(newType) },
                    enabled = (videoIndexStatus != ProcessorStatus.ACTIVE && imageIndexStatus != ProcessorStatus.ACTIVE)
                )

                if(searchResults.isNotEmpty()){
                    TextButton(onClick = {searchViewModel.clearResults() }) {
                        Text(stringResource(R.string.menu_clear_results))
                    }
                }
            }

            // ImageSearcher pro výběr obrázku (když je QueryType.IMAGE)
            if(queryType == QueryType.IMAGE){
                ImageSearcher(
                    uri = searchImageUri,
                    threshold = appSettings.similarityThreshold,
                    mediaType = mediaType,
                    searchEnabled = canSearch && searchImageUri != null,
                    mediaTypeSelectorEnabled = false,
                    hasCroppedImage = croppedBitmap != null,
                    onSearch = { threshold -> searchViewModel.imageSearch(threshold) },
                    onMediaTypeChange = { /* Handled by MediaTypeToggle */ },
                    onImageSelected = searchViewModel::updateSearchImageUri,
                    onCropClick = { searchViewModel.showCropDialog() },
                    onClearCrop = { searchViewModel.clearCrop() }
                )
            }

            // Filtry jsou nyní v Bottom Sheet, otevíraném pomocí FAB
            // FilterSection odstraněna z hlavního layoutu

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
                toggleViewResult = { uri -> searchViewModel.toggleViewResult(uri) },
                onLoadMore = searchViewModel::onLoadMore,
                totalResults=totalResults,
                loadMoreBuffer = (RESULTS_BATCH_SIZE * 0.2).toInt(),
                isSelectionMode = isSelectionMode,
                selectedUris = selectedUris,
                onToggleSelection = { uri -> searchViewModel.toggleUriSelection(uri) },
                onLongPress = { uri ->
                    searchViewModel.toggleSelectionMode()
                    searchViewModel.toggleUriSelection(uri)
                },
                onOpenViewer = { index -> searchViewModel.openViewer(index) }
            )
        }
        }

        // Success messages - musí být v outer Box scope
        showMoveSuccessMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        showDeleteSuccessMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Nový swipeable viewer
        viewerIndex?.let { index ->
            if (searchResults.isNotEmpty() && index < searchResults.size) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
                ) {
                    SwipeableMediaViewer(
                        uris = searchResults,
                        initialIndex = index,
                        type = mediaType,
                        onClose = { searchViewModel.closeViewer() }
                    )
                }
            }
        }

        // Fallback na starý viewer (pro zpětnou kompatibilitu)
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
                )
            }
        }

        // Filter FAB - pouze pro IMAGE mode
        if (mediaType == MediaType.IMAGE && !isSelectionMode) {
            val activeFiltersCount = selectedTagFilters.size +
                                    (if (selectedFewShotPrototype != null) 1 else 0) +
                                    (if (dateRangeStart != null || dateRangeEnd != null) 1 else 0)

            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { showFilterBottomSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown, // TODO: Replace with Filter icon
                    contentDescription = stringResource(R.string.filters)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.filters))
                if (activeFiltersCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(activeFiltersCount.toString())
                    }
                }
            }
        }
    }

    // Crop Image Dialog
    if (showCropDialog) {
        searchImageUri?.let { uri ->
            com.fpf.smartscan.ui.components.media.CropImageDialog(
                imageUri = uri,
                onCropped = { croppedBitmap ->
                    searchViewModel.setCroppedBitmap(croppedBitmap)
                },
                onDismiss = { searchViewModel.hideCropDialog() }
            )
        }
    }

    // Date Range Filter Dialog
    if (showDateRangeDialog) {
        DateRangeFilterDialog(
            currentStartDate = dateRangeStart,
            currentEndDate = dateRangeEnd,
            onDismiss = { showDateRangeDialog = false },
            onConfirm = { start, end ->
                searchViewModel.setDateRange(start, end)
            },
            onClear = {
                searchViewModel.clearDateRange()
            }
        )
    }

    // Filter Bottom Sheet
    if (showFilterBottomSheet && mediaType == MediaType.IMAGE) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(
                skipPartiallyExpanded = false
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.filters_and_options),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // FilterSection content (bez rozbalovacího wrapperu)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Tagy
                    if (availableTagsWithCounts.isNotEmpty()) {
                        TagFilterChips(
                            availableTags = availableTagsWithCounts,
                            selectedTags = selectedTagFilters,
                            onTagToggle = { tagName -> searchViewModel.toggleTagFilter(tagName) }
                        )
                    }

                    // 2. Few-Shot
                    if (availableFewShotPrototypes.isNotEmpty()) {
                        FewShotSelector(
                            prototypes = availableFewShotPrototypes,
                            selectedPrototype = selectedFewShotPrototype,
                            onPrototypeSelected = { prototype ->
                                searchViewModel.selectFewShotPrototype(prototype)
                            },
                            onSearchTriggered = {
                                searchViewModel.fewShotSearch(appSettings.similarityThreshold)
                                showFilterBottomSheet = false
                            }
                        )
                    }

                    // 3. Date range button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.FilterChip(
                            selected = dateRangeStart != null || dateRangeEnd != null,
                            onClick = { showDateRangeDialog = true },
                            label = {
                                Text(
                                    text = if (dateRangeStart != null || dateRangeEnd != null) {
                                        getDateRangeDescription(dateRangeStart, dateRangeEnd)
                                    } else {
                                        stringResource(R.string.date_filter_select)
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = stringResource(R.string.date_filter_label)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (dateRangeStart != null || dateRangeEnd != null) {
                            androidx.compose.material3.IconButton(
                                onClick = { searchViewModel.clearDateRange() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.date_filter_clear)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
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

/**
 * Tlačítko pro otevření date range filtru
 */
/**
 * Přepínač pro Media Type (IMAGE ↔ VIDEO)
 *
 * Zobrazuje switch s ikonkami obrázku a videa na stranách.
 */
@Composable
fun MediaTypeToggle(
    currentMediaType: MediaType,
    onMediaTypeChange: (MediaType) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ikona IMAGE (vlevo) - menší pro kompaktnost
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = "Image media",
            tint = if (currentMediaType == MediaType.IMAGE && enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.5f else 0.3f)
            },
            modifier = Modifier.size(14.dp)
        )

        // Switch (menší)
        Switch(
            checked = currentMediaType == MediaType.VIDEO,
            onCheckedChange = { isVideo ->
                if (enabled) {
                    onMediaTypeChange(if (isVideo) MediaType.VIDEO else MediaType.IMAGE)
                }
            },
            enabled = enabled,
            modifier = Modifier.scale(0.7f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )

        // Ikona VIDEO (vpravo) - menší pro kompaktnost
        Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = "Video media",
            tint = if (currentMediaType == MediaType.VIDEO && enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.5f else 0.3f)
            },
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * Přepínač pro Query Type (TEXT ↔ IMAGE search)
 *
 * Zobrazuje switch s ikonkami textu a obrázku na stranách.
 * Default je IMAGE (obrázky).
 */
@Composable
fun QueryTypeToggle(
    currentQueryType: QueryType,
    onQueryTypeChange: (QueryType) -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ikona TEXT (vlevo) - menší pro kompaktnost
        Icon(
            imageVector = Icons.Default.TextFields,
            contentDescription = "Text search",
            tint = if (currentQueryType == QueryType.TEXT) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(14.dp)
        )

        // Switch (menší)
        Switch(
            checked = currentQueryType == QueryType.IMAGE,
            onCheckedChange = { isImage ->
                onQueryTypeChange(if (isImage) QueryType.IMAGE else QueryType.TEXT)
            },
            modifier = Modifier.scale(0.7f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        )

        // Ikona IMAGE (vpravo) - menší pro kompaktnost
        Icon(
            imageVector = Icons.Default.ImageSearch,
            contentDescription = "Image search",
            tint = if (currentQueryType == QueryType.IMAGE) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun DateRangeFilterButton(
    currentStartDate: Long?,
    currentEndDate: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasFilter = currentStartDate != null || currentEndDate != null
    val description = getDateRangeDescription(currentStartDate, currentEndDate)

    Row(
        modifier = modifier
            .wrapContentWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.FilterChip(
            selected = hasFilter,
            onClick = onClick,
            label = {
                Text(
                    text = if (hasFilter) description else "Filtrovat podle data"
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Kalendář"
                )
            },
            trailingIcon = if (hasFilter) {
                {
                    androidx.compose.material3.IconButton(
                        onClick = { onClear() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Vymazat filter",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null
        )
    }
}

