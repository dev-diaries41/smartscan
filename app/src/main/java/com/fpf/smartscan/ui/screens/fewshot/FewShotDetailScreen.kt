package com.fpf.smartscan.ui.screens.fewshot

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.fpf.smartscan.data.fewshot.FewShotSampleEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen pro zobrazení detailu few-shot prototype
 *
 * Zobrazuje:
 * - Informace o prototypu (název, barva, počet samplu)
 * - Grid všech sample obrázků
 * - Možnost přidat další sample
 * - Možnost smazat sample
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FewShotDetailScreen(
    prototypeId: Long,
    viewModel: FewShotViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val prototype by viewModel.selectedPrototype.collectAsState()
    val samples by viewModel.selectedPrototypeSamples.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // State pro delete dialog
    var sampleToDelete by remember { mutableStateOf<FewShotSampleEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Načtení prototypu při zobrazení
    LaunchedEffect(prototypeId) {
        viewModel.selectPrototype(prototypeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(prototype?.name ?: "Detail")
                        Text(
                            text = "${samples.size} ${if (samples.size == 1) "sample" else "samples"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // TODO: Přidat nové sample
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Přidat sample")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Chyba: $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                }
                prototype == null -> {
                    Text(
                        text = "Prototype nenalezen",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Prototype info card
                        item {
                            PrototypeInfoCard(
                                name = prototype!!.name,
                                color = Color(prototype!!.color),
                                description = prototype!!.description,
                                category = prototype!!.category,
                                createdAt = prototype!!.createdAt
                            )
                        }

                        // Samples section header
                        item {
                            Text(
                                text = "Samples (${samples.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Samples grid
                        items(samples) { sample ->
                            SampleCard(
                                sample = sample,
                                onDeleteClick = {
                                    sampleToDelete = sample
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && sampleToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                sampleToDelete = null
            },
            title = { Text("Smazat sample?") },
            text = {
                Text("Opravdu chcete odstranit tento sample? Průměrný embedding bude přepočítán.")
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        sampleToDelete = null
                    }
                ) {
                    Text("Zrušit")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Implement sample deletion
                        // viewModel.removeSample(sampleToDelete!!.id, prototypeId)
                        showDeleteDialog = false
                        sampleToDelete = null
                    }
                ) {
                    Text("Smazat", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

/**
 * Card s informacemi o prototypu
 */
@Composable
private fun PrototypeInfoCard(
    name: String,
    color: Color,
    description: String?,
    category: String?,
    createdAt: Long
) {
    val dateFormat = remember { SimpleDateFormat("d.M.yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    category?.let { cat ->
                        Text(
                            text = cat.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Vytvořeno ${dateFormat.format(Date(createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Card pro zobrazení jednotlivého samplu
 */
@Composable
private fun SampleCard(
    sample: FewShotSampleEntity,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d.M.yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Image(
                painter = rememberAsyncImagePainter(
                    model = Uri.parse(sample.imageUri)
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Sample ${sample.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Přidáno ${dateFormat.format(Date(sample.addedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Embedding: ${sample.embedding.size} dim",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Delete button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Smazat sample",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
