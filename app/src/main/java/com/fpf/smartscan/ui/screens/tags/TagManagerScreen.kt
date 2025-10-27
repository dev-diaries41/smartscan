package com.fpf.smartscan.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fpf.smartscan.data.tags.UserTagEntity
import com.fpf.smartscan.workers.RetaggingWorker
import kotlinx.coroutines.launch

/**
 * Screen pro správu user-defined tagů
 *
 * Zobrazuje seznam všech tagů s možností CRUD operací:
 * - Create: FAB tlačítko pro nový tag
 * - Read: Seznam tagů s počtem obrázků
 * - Update: Edit button naviguje na TagEditScreen
 * - Delete: Delete button s confirmation dialogem
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen(
    tagViewModel: TagViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String?) -> Unit // null = nový tag, String = edit existujícího
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tagsWithCounts by tagViewModel.tagsWithCounts.collectAsState()
    val isLoading by tagViewModel.isLoading.collectAsState()
    val error by tagViewModel.error.collectAsState()

    // State pro delete dialog
    var tagToDelete by remember { mutableStateOf<UserTagEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // State pro re-tagging confirmation
    var showRetagDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Správa tagů")
                        if (tagsWithCounts.isNotEmpty()) {
                            Text(
                                text = "${tagsWithCounts.size} ${if (tagsWithCounts.size == 1) "tag" else if (tagsWithCounts.size < 5) "tagy" else "tagů"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    // Re-tagging button
                    IconButton(
                        onClick = { showRetagDialog = true },
                        enabled = tagsWithCounts.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Re-taggovat všechny obrázky",
                            tint = if (tagsWithCounts.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Přidat tag")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && tagsWithCounts.isEmpty() -> {
                    // Loading state pro první načtení
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                tagsWithCounts.isEmpty() -> {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📋",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Zatím nemáte žádné tagy",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vytvořte svůj první tag pro automatickou kategorizaci obrázků",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // Seznam tagů
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Error message
                        error?.let { errorMessage ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = errorMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        items(
                            items = tagsWithCounts,
                            key = { (tag, _) -> tag.name }
                        ) { (tag, imageCount) ->
                            TagCard(
                                tag = tag,
                                imageCount = imageCount,
                                onEdit = { onNavigateToEdit(tag.name) },
                                onDelete = {
                                    tagToDelete = tag
                                    showDeleteDialog = true
                                }
                            )
                        }

                        // Spacer pro FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Delete confirmation dialog
            if (showDeleteDialog && tagToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Smazat tag?") },
                    text = {
                        Text("Opravdu chcete smazat tag \"${tagToDelete!!.name}\"? Tato akce také odstraní všechna přiřazení tohoto tagu k obrázkům.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    tagToDelete?.let { tag ->
                                        tagViewModel.deleteTag(tag)
                                    }
                                    showDeleteDialog = false
                                    tagToDelete = null
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Smazat")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                tagToDelete = null
                            }
                        ) {
                            Text("Zrušit")
                        }
                    }
                )
            }

            // Re-tagging confirmation dialog
            if (showRetagDialog) {
                AlertDialog(
                    onDismissRequest = { showRetagDialog = false },
                    title = { Text("Re-taggovat všechny obrázky?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tato operace přepočítá tagy pro všechny indexované obrázky podle aktuálních pravidel.")
                            Text(
                                text = "Proces může trvat několik minut v závislosti na počtu obrázků.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Spuštění RetaggingWorker
                                val workRequest = OneTimeWorkRequestBuilder<RetaggingWorker>()
                                    .build()

                                WorkManager.getInstance(context)
                                    .enqueue(workRequest)

                                showRetagDialog = false
                            }
                        ) {
                            Text("Spustit")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showRetagDialog = false }
                        ) {
                            Text("Zrušit")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Karta pro jeden tag
 */
@Composable
private fun TagCard(
    tag: UserTagEntity,
    imageCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: název + color indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Color indicator
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(tag.color))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )

                    // Název tagu
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active badge
                if (!tag.isActive) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Neaktivní",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Statistika
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Počet obrázků
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📷",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$imageCount ${if (imageCount == 1) "obrázek" else if (imageCount < 5) "obrázky" else "obrázků"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Threshold
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Práh: ${String.format("%.2f", tag.threshold)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Popis (zkrácený)
            Text(
                text = tag.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upravit")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Smazat")
                }
            }
        }
    }
}
