package com.fpf.smartscan.ui.components.models

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.data.ModelInfo

@Composable
fun ModelsList(
    models: List<ModelInfo>,
    onDownload: (url: String) -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.verticalScroll(scrollState).fillMaxSize()) {
        Text(text = stringResource(R.string.setting_models_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.alpha(0.8f).padding(bottom = 16.dp),
        )
        models.forEach { model -> DownloadableModelCard(data = model, onDownload = onDownload) }
    }
}
