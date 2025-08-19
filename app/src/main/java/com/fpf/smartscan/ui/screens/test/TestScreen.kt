package com.fpf.smartscan.ui.screens.test

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.ui.components.ImageUploader
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(viewModel: TestViewModel = viewModel(), settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val imageUri by viewModel.imageUri.collectAsState(null)
    val inferenceResult by viewModel.predictedClass.collectAsState(null)
    val prototypes by settingsViewModel.prototypeList.collectAsState(emptyList())

    Box(
        modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ImageUploader(
                    imageUri = imageUri,
                    onImageSelected = { uri ->
                        viewModel.updateImageUri(uri)
                        if (uri == null) {
                            viewModel.clearInferenceResult()
                        }
                    }
                )

                if (imageUri != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.inference(context, prototypes)
                        },
                    ) {
                        Text("Classify Image")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                inferenceResult?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Result: $it")
                }
            }
        }
    }
