package com.fpf.smartscan.ui.components.models

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.fpf.smartscan.data.DownloadableModel

@Composable
fun ModelsList(
    models: List<DownloadableModel>,
    onDownload: (url: String) -> Unit,
) {
    LazyColumn{
        items(
            items = models,
            key = { it.name }
        ) { item -> DownloadableModelCard(data = item, onDownload = onDownload) }
    }
}
