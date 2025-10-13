package com.fpf.smartscan.ui.screens.test

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.data.prototypes.toEmbedding
import com.fpf.smartscan.ui.components.media.ImageUploader
import com.fpf.smartscan.ui.components.LoadingIndicator
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(viewModel: TestViewModel = viewModel(), settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val imageUri by viewModel.imageUri.collectAsState(null)
    val inferenceResult by viewModel.predictedClass.collectAsState(null)
    val prototypesEntities by settingsViewModel.prototypeList.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val classPrototypes = prototypesEntities.map { it.toEmbedding() }
    val isLoading by viewModel.isLoading.collectAsState(false)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDispose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ImageUploader(
                    size = 300,
                    uri = imageUri,
                    onImageSelected = { uri ->
                        viewModel.updateImageUri(uri)
                        if (uri == null) {
                            viewModel.clearInferenceResult()
                        }
                    },
                    placeholderText = {
                        Text(text = "Upload image",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.alpha(0.8f).padding(8.dp),
                        )
                    }
                )
                Button(
                    enabled = !isLoading && imageUri != null ,
                    modifier = Modifier.padding(vertical = 16.dp).width(300.dp),
                    onClick = { viewModel.inference(context, classPrototypes, threshold = appSettings.organiserSimilarityThreshold, confidenceMargin = appSettings.organiserConfMargin) },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Label icon", modifier = Modifier.padding(end = 4.dp))
                    Text("Classify")
                    LoadingIndicator(isVisible = isLoading, size = 18.dp, strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(start = 8.dp))
                }
                inferenceResult?.let { Text(text = "Result: $it", modifier = Modifier.padding(vertical = 16.dp)) }
            }
        }
    }