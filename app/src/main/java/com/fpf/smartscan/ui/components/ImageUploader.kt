package com.fpf.smartscan.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ImageUploader(
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    ) {
    val context = LocalContext.current

    // Load the image as an ImageBitmap using BitmapFactory
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = imageUri) {
        value = null
        imageUri?.let { uri ->
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    value = bitmap?.asImageBitmap()
                }
            }
        }
    }

    // Launcher to pick an image from the gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            onImageSelected(uri)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Image display area (with a Card container)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (imageUri != null) 8.dp else 4.dp)
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "Uploaded Image",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No image selected",
                            modifier = Modifier
                                .padding(bottom = 16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (imageUri == null){
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") }
                            ) {
                                Text(text = "Upload Image")
                            }
                        }
                    }
                }
            }

            // Close button in the top-right corner to remove the image
            if (imageUri != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp) // Move the entire circle left and down
                ) {
                    IconButton(
                        onClick = { onImageSelected(null) },
                        modifier = Modifier
                            .clip(CircleShape) // Makes the background circular
                            .background(MaterialTheme.colorScheme.onSecondaryContainer) // Dark grey background
                            .size(24.dp) // Adjust the size of the circle
                            .padding(4.dp) // Adds padding around the icon
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove Image",
                            tint = MaterialTheme.colorScheme.inversePrimary,
                        )
                    }
                }
            }
        }
    }
}
