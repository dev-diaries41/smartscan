package com.fpf.smartscan.ui.screens.fewshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.fpf.smartscan.data.fewshot.CropRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Dialog pro crop obrázku
 *
 * Umožňuje uživateli označit oblast objektu pro few-shot learning.
 * Vybraná oblast se uloží jako CropRect JSON.
 */
@Composable
fun ImageCropDialog(
    imageUri: Uri,
    currentIndex: Int,
    totalImages: Int,
    onDismiss: () -> Unit,
    onCropConfirmed: (CropRect) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Image size state
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var displaySize by remember { mutableStateOf(IntSize.Zero) }

    // Crop rectangle state (in display coordinates)
    var cropRect by remember {
        mutableStateOf(
            Rect(
                offset = Offset(50f, 50f),
                size = Size(200f, 200f)
            )
        )
    }

    // Drag state
    var isDragging by remember { mutableStateOf(false) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }

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
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Označte oblast objektu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Obrázek ${currentIndex + 1}/$totalImages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Táhněte pro vytvoření obdélníku kolem objektu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider()

                // Image with crop overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background image
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = imageUri
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                displaySize = coordinates.size

                                // Initialize crop rect to center if first time
                                if (imageSize == IntSize.Zero) {
                                    val centerX = displaySize.width / 2f
                                    val centerY = displaySize.height / 2f
                                    val size = min(displaySize.width, displaySize.height) * 0.6f
                                    cropRect = Rect(
                                        offset = Offset(centerX - size / 2, centerY - size / 2),
                                        size = Size(size, size)
                                    )
                                }
                            },
                        contentScale = ContentScale.Fit
                    )

                    // Crop overlay
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        dragStartOffset = offset
                                        cropRect = Rect(
                                            offset = offset,
                                            size = Size.Zero
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        val currentOffset = change.position

                                        val left = min(dragStartOffset.x, currentOffset.x)
                                        val top = min(dragStartOffset.y, currentOffset.y)
                                        val right = max(dragStartOffset.x, currentOffset.x)
                                        val bottom = max(dragStartOffset.y, currentOffset.y)

                                        cropRect = Rect(
                                            offset = Offset(left, top),
                                            size = Size(right - left, bottom - top)
                                        )
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                    }
                                )
                            }
                    ) {
                        // Draw semi-transparent overlay outside crop area
                        drawRect(
                            color = Color.Black.copy(alpha = 0.5f),
                            size = size
                        )

                        // Clear crop area
                        drawRect(
                            color = Color.Transparent,
                            topLeft = cropRect.topLeft,
                            size = cropRect.size,
                            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                        )

                        // Draw crop rectangle border
                        drawRect(
                            color = Color.White,
                            topLeft = cropRect.topLeft,
                            size = cropRect.size,
                            style = Stroke(width = 3f)
                        )

                        // Draw corner indicators
                        val cornerSize = 20f
                        val corners = listOf(
                            cropRect.topLeft,
                            Offset(cropRect.right, cropRect.top),
                            Offset(cropRect.left, cropRect.bottom),
                            Offset(cropRect.right, cropRect.bottom)
                        )

                        corners.forEach { corner ->
                            drawCircle(
                                color = Color.White,
                                radius = cornerSize / 2,
                                center = corner
                            )
                        }
                    }
                }

                Divider()

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Zrušit vše")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onSkip) {
                        Text("Přeskočit")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val actualCropRect = calculateActualCropRect(
                                    context = context,
                                    imageUri = imageUri,
                                    displaySize = displaySize,
                                    cropRect = cropRect
                                )
                                onCropConfirmed(actualCropRect)
                            }
                        },
                        enabled = cropRect.size.width > 20 && cropRect.size.height > 20
                    ) {
                        Text(
                            if (currentIndex < totalImages - 1) "Další →" else "Dokončit"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vypočítá skutečnou crop oblast v pixelech původního obrázku
 */
private suspend fun calculateActualCropRect(
    context: android.content.Context,
    imageUri: Uri,
    displaySize: IntSize,
    cropRect: Rect
): CropRect = withContext(Dispatchers.IO) {
    // Load image to get actual dimensions
    val inputStream = context.contentResolver.openInputStream(imageUri)
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeStream(inputStream, null, options)
    inputStream?.close()

    val actualWidth = options.outWidth
    val actualHeight = options.outHeight

    // Calculate scale factors
    val displayAspect = displaySize.width.toFloat() / displaySize.height.toFloat()
    val imageAspect = actualWidth.toFloat() / actualHeight.toFloat()

    val (scaleX, scaleY, offsetX, offsetY) = if (imageAspect > displayAspect) {
        // Image is wider - fit to width
        val scale = actualWidth.toFloat() / displaySize.width.toFloat()
        val scaledHeight = actualHeight / scale
        val offsetY = (displaySize.height - scaledHeight) / 2f
        ScaleAndOffset(scale, scale, 0f, offsetY)
    } else {
        // Image is taller - fit to height
        val scale = actualHeight.toFloat() / displaySize.height.toFloat()
        val scaledWidth = actualWidth / scale
        val offsetX = (displaySize.width - scaledWidth) / 2f
        ScaleAndOffset(scale, scale, offsetX, 0f)
    }

    // Convert display coordinates to actual image coordinates
    val actualLeft = ((cropRect.left - offsetX) * scaleX).toInt().coerceIn(0, actualWidth)
    val actualTop = ((cropRect.top - offsetY) * scaleY).toInt().coerceIn(0, actualHeight)
    val actualRight = ((cropRect.right - offsetX) * scaleX).toInt().coerceIn(0, actualWidth)
    val actualBottom = ((cropRect.bottom - offsetY) * scaleY).toInt().coerceIn(0, actualHeight)

    CropRect(
        left = actualLeft,
        top = actualTop,
        width = (actualRight - actualLeft).coerceAtLeast(1),
        height = (actualBottom - actualTop).coerceAtLeast(1)
    )
}

/**
 * Data class pro scale a offset hodnoty
 */
private data class ScaleAndOffset(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Float,
    val offsetY: Float
)
