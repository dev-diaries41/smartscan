package com.fpf.smartscan.ui.components.models

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fpf.smartscan.data.DownloadableModel

@Composable
fun ModelsList(
    models: List<DownloadableModel>,
    onDownload: (url: String) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scrollState).fillMaxSize()) {
        models.forEach { model -> DownloadableModelCard(data = model, onDownload = onDownload) }
    }
}
