package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zavřít výběr"
                    )
                }
                Text(
                    text = "Vybráno: $selectedCount",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Vybrat vše"
                    )
                }
                IconButton(onClick = onShare, enabled = selectedCount > 0) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.action_share),
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                IconButton(onClick = onMove, enabled = selectedCount > 0) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMove,
                        contentDescription = "Přesunout",
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                IconButton(onClick = onDelete, enabled = selectedCount > 0) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Smazat",
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}
