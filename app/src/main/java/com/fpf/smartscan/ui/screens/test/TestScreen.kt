package com.fpf.smartscan.ui.screens.test

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.data.prototypes.toEmbedding
import com.fpf.smartscan.ui.components.NewImageUploader
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

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDispose()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NewImageUploader(
                    uri = imageUri,
                    onImageSelected = { uri ->
                        viewModel.updateImageUri(uri)
                        if (uri == null) {
                            viewModel.clearInferenceResult()
                        }
                    },
                    placeholderText = {
                        Text(text = "Upload image",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.alpha(0.8f).padding(8.dp),
                        )
                    }
                )
                if (imageUri != null) {
                    Button(modifier = Modifier.padding(vertical = 16.dp),
                        onClick = { viewModel.inference(context, classPrototypes, threshold = appSettings.organiserSimilarityThreshold, confidenceMargin = appSettings.organiserConfMargin) },
                    ) { Text("Classify Image") }
                }
                inferenceResult?.let { Text(text = "Result: $it", modifier = Modifier.padding(vertical = 16.dp)) }
            }
        }
    }