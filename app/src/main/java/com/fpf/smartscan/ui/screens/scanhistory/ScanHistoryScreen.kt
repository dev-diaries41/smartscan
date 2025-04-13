package com.fpf.smartscan.ui.screens.scanhistory

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.fpf.smartscan.data.scans.ScanData
import com.fpf.smartscan.R
import com.fpf.smartscan.lib.toDateString

@Composable
fun ScanHistoryScreen(viewModel: ScanHistoryViewModel = viewModel()) {
    val items by viewModel.scanDataList.observeAsState(initial = emptyList())
    
    if (items.isEmpty()) {
        EmptyScanHistoryScreen()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp)
        ) {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                ScanHistoryItemCard(data = item)
            }
        }
    }
}

@Composable
fun ScanHistoryItemCard(data: ScanData) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Folder icon",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Scan results",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Date: ${toDateString(data.date)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.alpha(0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (data.result == ScanData.ERROR_RESULT) {
                    Text(
                        text = "Images moved: unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: failed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Images moved: ${data.result}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: success",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

        }
    }

}

@Composable
fun EmptyScanHistoryScreen() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "History icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(96.dp)
                    .rotate(rotation) // Apply the rotation animation here.
            )
            Text(
                text = stringResource(R.string.no_scan_history),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = stringResource(R.string.no_scan_history_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(0.8f)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

