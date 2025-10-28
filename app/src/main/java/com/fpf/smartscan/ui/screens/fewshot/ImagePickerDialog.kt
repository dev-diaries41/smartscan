package com.fpf.smartscan.ui.screens.fewshot

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dialog pro výběr obrázků z galerie
 *
 * Umožňuje uživateli vybrat 3-10 obrázků pro vytvoření few-shot tagu.
 * Po výběru otevře ImageCropDialog pro každý vybraný obrázek.
 */
@Composable
fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onImagesSelected: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    var images by remember { mutableStateOf<List<MediaImage>>(emptyList()) }
    var selectedImages by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    // Načtení obrázků při zobrazení dialogu
    LaunchedEffect(Unit) {
        images = loadImagesFromMediaStore(context)
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Vyberte obrázky",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedImages.size}/10 vybraných (min. 3)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedImages.size < 3) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Divider()

                // Image grid
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    images.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Žádné obrázky nenalezeny",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(images) { image ->
                                ImageGridItem(
                                    image = image,
                                    isSelected = image.uri in selectedImages,
                                    onClick = {
                                        selectedImages = if (image.uri in selectedImages) {
                                            selectedImages - image.uri
                                        } else {
                                            if (selectedImages.size < 10) {
                                                selectedImages + image.uri
                                            } else {
                                                selectedImages
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Divider()

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Zrušit")
                    }
                    Button(
                        onClick = {
                            onImagesSelected(selectedImages.toList())
                        },
                        enabled = selectedImages.size >= 3
                    ) {
                        Text("Pokračovat (${selectedImages.size})")
                    }
                }
            }
        }
    }
}

/**
 * Komponenta pro zobrazení jednoho obrázku v gridu
 */
@Composable
private fun ImageGridItem(
    image: MediaImage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = image.uri
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
            )
        }
    }
}

/**
 * Data class pro reprezentaci obrázku z MediaStore
 */
private data class MediaImage(
    val uri: Uri,
    val dateAdded: Long
)

/**
 * Načte obrázky z MediaStore
 */
private suspend fun loadImagesFromMediaStore(context: Context): List<MediaImage> =
    withContext(Dispatchers.IO) {
        val images = mutableListOf<MediaImage>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                images.add(MediaImage(uri, dateAdded))
            }
        }

        images
    }
