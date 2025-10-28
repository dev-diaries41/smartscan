package com.fpf.smartscan.ui.components.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fpf.smartscan.R
import com.fpf.smartscan.data.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Dialog pro crop obrázku - uživatel vybere obdélníkovou oblast
 *
 * @param imageUri URI obrázku k oříznutí
 * @param onCropped Callback s oříznutým Bitmap
 * @param onDismiss Callback pro zavření dialogu
 */
@Composable
fun CropImageDialog(
    imageUri: Uri,
    onCropped: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Načtení obrázku
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    originalBitmap = BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar s tlačítky
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = Color.White
                        )
                    }

                    Text(
                        text = stringResource(R.string.crop_select_area),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    // Prázdný prostor pro symetrii (tlačítko Crop je dole)
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Crop editor
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    originalBitmap?.let { bitmap ->
                        Log.i("CropImageDialog", "Initializing CropImageEditor with bitmap size: ${bitmap.width}x${bitmap.height}")
                        CropImageEditor(
                            bitmap = bitmap,
                            onCropConfirmed = { croppedBitmap ->
                                Log.i("CropImageDialog", "CropImageEditor confirmed. Calling parent onCropped callback")
                                onCropped(croppedBitmap)
                                Log.i("CropImageDialog", "Parent onCropped called. Dismissing dialog")
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Editor pro výběr crop oblasti pomocí tažení prstem
 */
@Composable
private fun CropImageEditor(
    bitmap: Bitmap,
    onCropConfirmed: (Bitmap) -> Unit
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var cropRect by remember { mutableStateOf<Rect?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Obrázek s crop overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Zobrazení obrázku
                ImageDisplay(
                    uri = null,
                    bitmap = bitmap,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            imageSize = size
                            // Inicializace crop rect na celý obrázek
                            if (cropRect == null) {
                                cropRect = Rect(
                                    offset = Offset.Zero,
                                    size = Size(size.width.toFloat(), size.height.toFloat())
                                )
                            }
                        },
                    contentScale = ContentScale.Fit,
                    type = MediaType.IMAGE
                )

                // Overlay pro kreslení crop obdélníku
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragStart = offset
                                    cropRect = Rect(offset, Size.Zero)
                                    Log.i("CropImageDialog", "Drag started at: $offset")
                                },
                                onDrag = { change, _ ->
                                    val currentPos = change.position
                                    val topLeft = Offset(
                                        x = min(dragStart.x, currentPos.x).coerceIn(0f, imageSize.width.toFloat()),
                                        y = min(dragStart.y, currentPos.y).coerceIn(0f, imageSize.height.toFloat())
                                    )
                                    val bottomRight = Offset(
                                        x = max(dragStart.x, currentPos.x).coerceIn(0f, imageSize.width.toFloat()),
                                        y = max(dragStart.y, currentPos.y).coerceIn(0f, imageSize.height.toFloat())
                                    )
                                    cropRect = Rect(
                                        topLeft,
                                        bottomRight
                                    )
                                },
                                onDragEnd = {
                                    isDragging = false
                                    Log.i("CropImageDialog", "Drag ended. Final cropRect: $cropRect")
                                }
                            )
                        }
                ) {
                    cropRect?.let { rect ->
                        // Tmavý overlay mimo crop oblast
                        drawRect(
                            color = Color.Black.copy(alpha = 0.5f),
                            topLeft = Offset.Zero,
                            size = Size(size.width, rect.top)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.5f),
                            topLeft = Offset(0f, rect.top),
                            size = Size(rect.left, rect.height)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.5f),
                            topLeft = Offset(rect.right, rect.top),
                            size = Size(size.width - rect.right, rect.height)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.5f),
                            topLeft = Offset(0f, rect.bottom),
                            size = Size(size.width, size.height - rect.bottom)
                        )

                        // Bílý rámeček kolem crop oblasti
                        drawRect(
                            color = Color.White,
                            topLeft = rect.topLeft,
                            size = rect.size,
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }

            // Tlačítko pro potvrzení crop
            Button(
                onClick = {
                    Log.i("CropImageDialog", "Crop button clicked. cropRect: $cropRect, imageSize: $imageSize")
                    cropRect?.let { rect ->
                        Log.i("CropImageDialog", "Cropping bitmap. Original size: ${bitmap.width}x${bitmap.height}, cropRect: $rect")
                        val croppedBitmap = cropBitmap(
                            bitmap = bitmap,
                            cropRect = rect,
                            displaySize = imageSize
                        )
                        Log.i("CropImageDialog", "Cropped bitmap created. Size: ${croppedBitmap.width}x${croppedBitmap.height}")
                        onCropConfirmed(croppedBitmap)
                        Log.i("CropImageDialog", "onCropConfirmed callback called")
                    } ?: Log.w("CropImageDialog", "cropRect is null, cannot crop")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                enabled = cropRect != null && cropRect!!.width > 0 && cropRect!!.height > 0
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.action_crop_and_search))
            }
        }
    }
}

/**
 * Helper funkce pro ImageDisplay s Bitmap parametrem
 */
@Composable
private fun ImageDisplay(
    uri: Uri?,
    bitmap: Bitmap? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    type: MediaType
) {
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (uri != null) {
        ImageDisplay(
            uri = uri,
            modifier = modifier,
            contentScale = contentScale,
            type = type
        )
    }
}

/**
 * Ořízne bitmap podle vybrané oblasti
 *
 * @param bitmap Originální bitmap
 * @param cropRect Obdélník crop oblasti v display coordinates
 * @param displaySize Velikost zobrazeného obrázku na obrazovce
 * @return Oříznutý bitmap
 */
private fun cropBitmap(
    bitmap: Bitmap,
    cropRect: Rect,
    displaySize: IntSize
): Bitmap {
    // Přepočet z display coordinates na bitmap coordinates
    val scaleX = bitmap.width.toFloat() / displaySize.width
    val scaleY = bitmap.height.toFloat() / displaySize.height

    val x = (cropRect.left * scaleX).toInt().coerceIn(0, bitmap.width)
    val y = (cropRect.top * scaleY).toInt().coerceIn(0, bitmap.height)
    val width = (cropRect.width * scaleX).toInt().coerceAtMost(bitmap.width - x)
    val height = (cropRect.height * scaleY).toInt().coerceAtMost(bitmap.height - y)

    // Minimální velikost 1x1
    val finalWidth = max(1, width)
    val finalHeight = max(1, height)

    return Bitmap.createBitmap(bitmap, x, y, finalWidth, finalHeight)
}
