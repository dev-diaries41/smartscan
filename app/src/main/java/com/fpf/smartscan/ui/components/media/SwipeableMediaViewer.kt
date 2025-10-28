package com.fpf.smartscan.ui.components.media

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.lib.DEFAULT_IMAGE_DISPLAY_SIZE
import com.fpf.smartscan.lib.canOpenUri
import com.fpf.smartscan.lib.getImageIdFromUri
import com.fpf.smartscan.lib.getMediaIdFromUri
import com.fpf.smartscan.lib.openImageInGallery
import com.fpf.smartscan.lib.openVideoInGallery
import com.fpf.smartscan.ui.screens.tags.TagViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableMediaViewer(
    uris: List<Uri>,
    initialIndex: Int,
    type: MediaType,
    onClose: () -> Unit,
    maxSize: Int = DEFAULT_IMAGE_DISPLAY_SIZE
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { uris.size }
    )
    var isActionsVisible by remember { mutableStateOf(true) }

    // Tag management state
    val tagViewModel: TagViewModel = viewModel()
    var showTagPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val allTags by tagViewModel.allTags.collectAsState(initial = emptyList())

    Popup(
        onDismissRequest = { onClose() },
        properties = PopupProperties(dismissOnBackPress = true, focusable = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val uri = uris[page]

                if (type == MediaType.IMAGE) {
                    ImageDisplay(
                        uri = uri,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { isActionsVisible = !isActionsVisible }
                                )
                            },
                        contentScale = ContentScale.FillWidth,
                        type = type,
                        maxSize = maxSize
                    )
                } else {
                    VideoDisplay(
                        uri = uri,
                        modifier = Modifier.fillMaxSize(),
                        onTap = { isActionsVisible = !isActionsVisible }
                    )
                }
            }

            // UI overlay (action bar + tags)
            val currentUri = uris[pagerState.currentPage]
            val currentMediaId = getMediaIdFromUri(currentUri)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top action bar
                SwipeableActionRow(
                    uri = currentUri,
                    type = type,
                    onClose = onClose,
                    isVisible = isActionsVisible,
                    currentIndex = pagerState.currentPage + 1,
                    totalCount = uris.size
                )

                // Bottom tags (pouze pro obrázky)
                if (currentMediaId != null && isActionsVisible) {
                    val currentTags by tagViewModel.getTagsForMedia(currentMediaId).collectAsState(initial = emptyList())

                    TagChipsRow(
                        imageId = currentMediaId,
                        tagViewModel = tagViewModel,
                        onAddTag = { showTagPicker = true },
                        onRemoveTag = { mediaId, tagName ->
                            scope.launch {
                                tagViewModel.removeTagFromMedia(mediaId, tagName)
                            }
                        }
                    )

                    // Tag picker dialog
                    if (showTagPicker) {
                        TagPickerDialog(
                            imageId = currentMediaId,
                            availableTags = allTags,
                            currentTags = currentTags,
                            onDismiss = { showTagPicker = false },
                            onTagSelected = { tag ->
                                scope.launch {
                                    tagViewModel.addTagToMedia(currentMediaId, tag.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeableActionRow(
    uri: Uri,
    type: MediaType,
    onClose: () -> Unit,
    isVisible: Boolean,
    currentIndex: Int,
    totalCount: Int
) {
    val context = LocalContext.current
    val mime = context.contentResolver.getType(uri)
    val shareIntent: Intent = Intent().apply {
        this.action = Intent.ACTION_SEND
        this.putExtra(Intent.EXTRA_STREAM, uri)
        this.type = mime
    }
    val clipboard = LocalClipboard.current
    val isUriAccessible = canOpenUri(context, uri)

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300), label = "alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -30f,
        animationSpec = tween(durationMillis = 300), label = "translationY"
    )

    if (alpha > 0f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = alpha
                    this.translationY = translationY
                }
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Levá strana - Back button
            IconButton(onClick = { onClose() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zavřít",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            // Střed - pozice indikátor
            Text(
                text = "$currentIndex / $totalCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Pravá strana - Akce
            if (isUriAccessible) {
                Row {
                    IconButton(onClick = {
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newUri(
                                context.contentResolver,
                                "smartscan_media",
                                uri
                            )
                        )
                    }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Kopírovat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = {
                        if (type == MediaType.IMAGE) {
                            openImageInGallery(context, uri)
                        } else {
                            openVideoInGallery(context, uri)
                        }
                    }) {
                        Icon(
                            Icons.Filled.PhotoLibrary,
                            contentDescription = "Otevřít v galerii",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    mime?.let {
                        IconButton(onClick = {
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Sdílet",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
